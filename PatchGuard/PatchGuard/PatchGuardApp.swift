//
//  PatchGuardApp.swift
//  PatchGuard
//

import SwiftUI

@main
struct PatchGuardApp: App {
    @State private var isLoggedIn = IngestService.isTestMode || KeychainService.load() != nil

    var body: some Scene {
        WindowGroup {
            if isLoggedIn {
                ContentView(onLogout: { isLoggedIn = false })
            } else {
                LoginView { isLoggedIn = true }
            }
        }
    }
}
