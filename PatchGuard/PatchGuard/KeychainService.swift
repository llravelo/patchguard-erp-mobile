//
//  KeychainService.swift
//  PatchGuard
//

import Foundation
import Security

enum KeychainService {
    private static let emailKey    = "pg.credentials.email"
    private static let passwordKey = "pg.credentials.password"

    static func save(email: String, password: String) {
        set(email,    for: emailKey)
        set(password, for: passwordKey)
    }

    static func load() -> (email: String, password: String)? {
        guard
            let email    = get(emailKey),
            let password = get(passwordKey)
        else { return nil }
        return (email, password)
    }

    static func clear() {
        delete(emailKey)
        delete(passwordKey)
    }

    // MARK: - Private

    private static func set(_ value: String, for key: String) {
        guard let data = value.data(using: .utf8) else { return }
        var query: [CFString: Any] = [
            kSecClass:       kSecClassGenericPassword,
            kSecAttrAccount: key,
        ]
        if SecItemCopyMatching(query as CFDictionary, nil) == errSecSuccess {
            SecItemUpdate(query as CFDictionary, [kSecValueData: data] as CFDictionary)
        } else {
            query[kSecValueData] = data
            SecItemAdd(query as CFDictionary, nil)
        }
    }

    private static func get(_ key: String) -> String? {
        let query: [CFString: Any] = [
            kSecClass:            kSecClassGenericPassword,
            kSecAttrAccount:      key,
            kSecReturnData:       true,
            kSecMatchLimit:       kSecMatchLimitOne,
        ]
        var result: AnyObject?
        guard SecItemCopyMatching(query as CFDictionary, &result) == errSecSuccess,
              let data = result as? Data
        else { return nil }
        return String(data: data, encoding: .utf8)
    }

    private static func delete(_ key: String) {
        let query: [CFString: Any] = [
            kSecClass:       kSecClassGenericPassword,
            kSecAttrAccount: key,
        ]
        SecItemDelete(query as CFDictionary)
    }
}
