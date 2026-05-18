//
//  IngestService.swift
//  PatchGuard
//

import Foundation

struct ImageMetadata: Encodable, Sendable {
    let filename: String
    let latitude: Double
    let longitude: Double
    let captured_at: String
    let heading: Double?
    let altitude: Double?
    let gps_accuracy: Double?
}

enum IngestService {
    private static let endpoint: URL = {
        let raw = Bundle.main.object(forInfoDictionaryKey: "SERVER_ENDPOINT") as? String
            ?? "http://192.168.0.21:3000/api/v1/images/batch"
        return URL(string: raw)!
    }()

    private static let session: URLSession = {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 15
        return URLSession(configuration: config)
    }()

    static func send(batch: [FrameBuffer.Frame]) {
        guard !batch.isEmpty else { return }

        let boundary = "PGBoundary-\(UUID().uuidString)"
        var body = Data()

        for frame in batch {
            body.appendString("--\(boundary)\r\n")
            body.appendString("Content-Disposition: form-data; name=\"files\"; filename=\"\(frame.metadata.filename)\"\r\n")
            body.appendString("Content-Type: image/jpeg\r\n\r\n")
            body.append(frame.jpeg)
            body.appendString("\r\n")
        }

        if let itemsJSON = try? JSONEncoder().encode(batch.map(\.metadata)) {
            body.appendString("--\(boundary)\r\n")
            body.appendString("Content-Disposition: form-data; name=\"items_json\"\r\n\r\n")
            body.append(itemsJSON)
            body.appendString("\r\n")
        }

        body.appendString("--\(boundary)--\r\n")

        var request = URLRequest(url: endpoint)
        request.httpMethod = "POST"
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
        request.httpBody = body

        session.dataTask(with: request) { _, response, error in
            if let error {
                print("[Ingest] \(error.localizedDescription)")
            } else if let http = response as? HTTPURLResponse, http.statusCode != 200 {
                print("[Ingest] HTTP \(http.statusCode)")
            }
        }.resume()
    }
}

private extension Data {
    mutating func appendString(_ string: String) {
        if let data = string.data(using: .utf8) { append(data) }
    }
}
