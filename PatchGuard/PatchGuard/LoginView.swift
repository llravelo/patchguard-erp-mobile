//
//  LoginView.swift
//  PatchGuard
//

import SwiftUI

struct LoginView: View {
    let onLogin: () -> Void

    @State private var email    = ""
    @State private var password = ""
    @State private var error: String?
    @State private var isLoading = false

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            VStack(spacing: 24) {
                Text("PatchGuard")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(.white)

                VStack(spacing: 12) {
                    TextField("Email", text: $email)
                        .keyboardType(.emailAddress)
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.never)
                        .textFieldStyle(PGTextFieldStyle())

                    SecureField("Password", text: $password)
                        .textFieldStyle(PGTextFieldStyle())
                }

                if let error {
                    Text(error)
                        .font(.system(size: 13))
                        .foregroundStyle(.red)
                        .multilineTextAlignment(.center)
                }

                Button {
                    Task { await login() }
                } label: {
                    Group {
                        if isLoading {
                            ProgressView().tint(.black)
                        } else {
                            Text("Sign In")
                                .font(.system(size: 16, weight: .semibold))
                        }
                    }
                    .frame(maxWidth: .infinity, minHeight: 50)
                    .background(Color.white)
                    .foregroundStyle(.black)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .disabled(email.isEmpty || password.isEmpty || isLoading)
            }
            .padding(32)
        }
    }

    private func login() async {
        isLoading = true
        error     = nil
        do {
            try await IngestService.login(email: email, password: password)
            KeychainService.save(email: email, password: password)
            onLogin()
        } catch {
            self.error = "Invalid email or password."
        }
        isLoading = false
    }
}

struct PGTextFieldStyle: TextFieldStyle {
    func _body(configuration: TextField<Self._Label>) -> some View {
        configuration
            .padding(14)
            .background(Color.white.opacity(0.1))
            .foregroundStyle(.white)
            .clipShape(RoundedRectangle(cornerRadius: 10))
            .tint(.white)
    }
}
