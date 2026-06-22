//
//  IngestService.swift
//  PatchGuard
//

import Foundation

enum IngestService {
    static let isTestMode: Bool =
        Bundle.main.object(forInfoDictionaryKey: "TEST_MODE") as? Bool ?? false

    private static let baseURL: URL = {
        let key = isTestMode ? "MOCK_SERVER_BASE_URL" : "SERVER_BASE_URL"
        let fallback = isTestMode ? "http://localhost:3000" : "http://192.168.0.21:8000"
        let raw = Bundle.main.object(forInfoDictionaryKey: key) as? String ?? fallback
        return URL(string: raw)!
    }()

    private static let session: URLSession = {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 15
        return URLSession(configuration: config)
    }()

    private static var accessToken: String?

    // MARK: - Auth

    static func login(email: String, password: String) async throws {
        accessToken = try await fetchToken(email: email, password: password)
    }

    private static func ensureToken() async throws {
        guard accessToken == nil else { return }
        guard let credentials = KeychainService.load() else {
            throw URLError(.userAuthenticationRequired)
        }
        accessToken = try await fetchToken(email: credentials.email, password: credentials.password)
    }

    private static func fetchToken(email: String, password: String) async throws -> String {
        let url = baseURL.appendingPathComponent("api/v1/auth/login")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        struct LoginBody: Encodable { let email: String; let password: String }
        request.httpBody = try JSONEncoder().encode(LoginBody(email: email, password: password))

        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse, http.statusCode == 200 else {
            let code = (response as? HTTPURLResponse)?.statusCode ?? 0
            print("[Ingest] Login failed: HTTP \(code)")
            throw URLError(.badServerResponse)
        }
        struct TokenResponse: Decodable { let token: String }
        return try JSONDecoder().decode(TokenResponse.self, from: data).token
    }

    // MARK: - Frame upload

    static func send(batch: [FrameBuffer.Frame]) async {
        guard !batch.isEmpty else { return }
        if !isTestMode {
            do { try await ensureToken() } catch {
                print("[Ingest] Auth failed: \(error.localizedDescription)")
                return
            }
        }

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

        let uploadPath = isTestMode ? "api/v1/images/batch" : "api/v1/mobile/frames"
        var request = URLRequest(url: baseURL.appendingPathComponent(uploadPath))
        request.httpMethod = "POST"
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
        if !isTestMode, let token = accessToken {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        request.httpBody = body

        do {
            let (_, response) = try await session.data(for: request)
            if let http = response as? HTTPURLResponse {
                if !isTestMode && http.statusCode == 401 { accessToken = nil }
                let expected = isTestMode ? 200 : 201
                if http.statusCode != expected {
                    print("[Ingest] Upload HTTP \(http.statusCode)")
                    return
                }
            }
        } catch {
            print("[Ingest] Upload failed: \(error.localizedDescription)")
            return
        }

        // Fire-and-forget: kick off background inference on the server.
        if !isTestMode {
            var triggerRequest = URLRequest(url: baseURL.appendingPathComponent("api/v1/analysis/trigger"))
            triggerRequest.httpMethod = "POST"
            if let token = accessToken {
                triggerRequest.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
            }
            _ = try? await session.data(for: triggerRequest)
        }
    }
}

private extension Data {
    mutating func appendString(_ string: String) {
        if let data = string.data(using: .utf8) { append(data) }
    }
}
