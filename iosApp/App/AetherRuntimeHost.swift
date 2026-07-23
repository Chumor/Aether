import Foundation
import UIKit
import UniformTypeIdentifiers
import AetherShared

final class AetherRuntimeHost: NSObject, NativeRuntimeHost, UIDocumentPickerDelegate {
    static let shared = AetherRuntimeHost()

    private let runtime = AetherISHRuntime.shared()
    private let operations = DispatchQueue(label: "com.baimoqilin.aether.runtime-host")
    private var initialized = false
    private var filePickerListener: NativePickedFileListener?

    func initialize(listener: NativeRuntimeInitializationListener) {
        operations.async { [self] in
            if initialized {
                onMain { listener.onReady() }
                return
            }
            onMain { listener.onProgress(phase: "rootfs", detail: "Preparing Alpine", fraction: 0.02) }
            runtime.initialize(
                progress: { phase, detail, fraction in
                    self.onMain {
                        listener.onProgress(phase: phase, detail: detail, fraction: fraction)
                    }
                },
                completion: { error in
                    if let error {
                        self.onMain { listener.onError(message: error.localizedDescription) }
                        return
                    }
                    self.finishInitialization(listener: listener)
                }
            )
        }
    }

    private func finishInitialization(listener: NativeRuntimeInitializationListener) {
        operations.async { [self] in
            do {
                let workspace = try workspaceURL()
                let chromeRuntime = try chromeRuntimeURL()
                let chromeDependencies = try chromeDependenciesURL()
                try guestCreateDirectories("/workspace")
                try guestBind(hostPath: workspace.path, guestPath: "/workspace")
                try guestCreateDirectories("/usr/lib/chromium")
                try guestBind(hostPath: chromeRuntime.path, guestPath: "/usr/lib/chromium")
                try guestCreateDirectories("/opt/aether/chromium-deps")
                try guestBind(hostPath: chromeDependencies.path, guestPath: "/opt/aether/chromium-deps")
                try installBridgeAsset()
                try installNodeCompatibilityAssets()
            } catch {
                onMain { listener.onError(message: error.localizedDescription) }
                return
            }

            onMain { listener.onProgress(phase: "node", detail: "Checking Node 22", fraction: 0.82) }
            let command = "node --version 2>/dev/null | grep -q '^v22\\.' || apk add --no-cache nodejs npm"
            let pid = runtime.startExecutable(
                "/bin/sh",
                arguments: ["-c", command],
                environment: [:],
                workingDirectory: "/root",
                pseudoTerminal: false,
                remoteDebuggingPipe: false,
                standardOutput: { data in
                    self.forwardSetupOutput(data, listener: listener)
                },
                standardError: { data in
                    self.forwardSetupOutput(data, listener: listener)
                },
                exit: { code, _ in
                    guard code == 0 else {
                        self.onMain { listener.onError(message: "Unable to install Node 22 in Alpine (exit \(code)).") }
                        return
                    }
                    self.operations.async {
                        self.initialized = true
                        self.onMain {
                            listener.onProgress(phase: "ready", detail: "Alpine is ready", fraction: 1.0)
                            listener.onReady()
                        }
                    }
                }
            )
            if pid < 0 {
                onMain { listener.onError(message: "Unable to start Alpine package setup (\(pid)).") }
            }
        }
    }

    private func forwardSetupOutput(
        _ data: Data,
        listener: NativeRuntimeInitializationListener
    ) {
        guard !data.isEmpty else { return }
        let text = String(decoding: data, as: UTF8.self)
        onMain { listener.onOutput(text: text) }
    }

    func startProcess(
        executable: String,
        arguments: [String],
        environment: [String: String],
        workingDirectory: String,
        redirectErrorStream: Bool,
        interactiveTerminal: Bool,
        remoteDebuggingPipe: Bool,
        listener: NativeRuntimeProcessListener
    ) -> Int64 {
        let stdout: AetherISHOutputBlock = { data in
            listener.onStdout(bytes: data.kotlinByteArray)
        }
        let stderr: AetherISHOutputBlock = { data in
            if redirectErrorStream {
                listener.onStdout(bytes: data.kotlinByteArray)
            } else {
                listener.onStderr(bytes: data.kotlinByteArray)
            }
        }
        let pid = runtime.startExecutable(
            executable,
            arguments: arguments,
            environment: environment,
            workingDirectory: workingDirectory,
            pseudoTerminal: interactiveTerminal,
            remoteDebuggingPipe: remoteDebuggingPipe,
            standardOutput: stdout,
            standardError: stderr,
            exit: { exitCode, signal in
                listener.onExit(exitCode: exitCode, signal: signal)
            }
        )
        return Int64(pid)
    }

    func writeStdin(processId: Int64, bytes: KotlinByteArray) -> Bool {
        runtime.writeStdin(bytes.data, processId: Int32(processId))
    }

    func closeStdin(processId: Int64) {
        runtime.closeStdin(forProcessId: Int32(processId))
    }

    func signal(processId: Int64, signal: Int32) {
        runtime.signalProcessId(Int32(processId), signal: signal)
    }

    func resizeTerminal(processId: Int64, columns: Int32, rows: Int32) {
        runtime.resizeTerminal(forProcessId: Int32(processId), columns: columns, rows: rows)
    }

    func fileExists(path: String, listener: NativeBooleanResultListener) {
        operations.async { [self] in
            complete { listener.onSuccess(value: self.runtime.fileExists(path)) }
        }
    }

    func createDirectories(path: String, listener: NativeUnitResultListener) {
        operations.async { [self] in
            complete(listener: listener) { try guestCreateDirectories(path) }
        }
    }

    func readFile(path: String, listener: NativeBytesResultListener) {
        operations.async { [self] in
            do {
                let data = try runtime.readFile(path)
                complete { listener.onSuccess(value_: data.kotlinByteArray) }
            } catch {
                complete { listener.onError(message: error.localizedDescription) }
            }
        }
    }

    func writeFile(path: String, bytes: KotlinByteArray, executable: Bool, listener: NativeUnitResultListener) {
        operations.async { [self] in
            complete(listener: listener) {
                try runtime.writeFile(path, data: bytes.data, executable: executable)
            }
        }
    }

    func remove(path: String, recursive: Bool, listener: NativeUnitResultListener) {
        operations.async { [self] in
            complete(listener: listener) {
                try runtime.removePath(path, recursive: recursive)
            }
        }
    }

    func bindHostDirectory(hostPath: String, guestPath: String, readOnly: Bool, listener: NativeUnitResultListener) {
        operations.async { [self] in
            complete(listener: listener) { try guestBind(hostPath: hostPath, guestPath: guestPath, readOnly: readOnly) }
        }
    }

    func pickFile(imagesOnly: Bool, listener: NativePickedFileListener) {
        onMain { [self] in
            guard filePickerListener == nil else {
                listener.onError(message: "Another file picker is already open.")
                return
            }
            guard let presenter = topViewController() else {
                listener.onError(message: "Unable to present the file picker.")
                return
            }
            filePickerListener = listener
            let types: [UTType] = imagesOnly ? [.image] : [.item]
            let picker = UIDocumentPickerViewController(forOpeningContentTypes: types, asCopy: true)
            picker.delegate = self
            picker.allowsMultipleSelection = false
            presenter.present(picker, animated: true)
        }
    }

    func openUrl(url: String) -> Bool {
        guard let target = URL(string: url), UIApplication.shared.canOpenURL(target) else { return false }
        onMain { UIApplication.shared.open(target) }
        return true
    }

    func doCopyText(text: String) -> Bool {
        UIPasteboard.general.string = text
        return true
    }

    func shareText(title: String, text: String) -> Bool {
        guard let presenter = topViewController() else { return false }
        let controller = UIActivityViewController(activityItems: [text], applicationActivities: nil)
        controller.title = title
        if let popover = controller.popoverPresentationController {
            popover.sourceView = presenter.view
            popover.sourceRect = CGRect(
                x: presenter.view.bounds.midX,
                y: presenter.view.bounds.maxY - 1,
                width: 1,
                height: 1
            )
        }
        presenter.present(controller, animated: true)
        return true
    }

    func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
        guard let listener = takeFilePickerListener() else { return }
        guard let url = urls.first else {
            listener.onCancelled()
            return
        }
        let scoped = url.startAccessingSecurityScopedResource()
        defer { if scoped { url.stopAccessingSecurityScopedResource() } }
        do {
            let data = try Data(contentsOf: url, options: .mappedIfSafe)
            let mimeType = UTType(filenameExtension: url.pathExtension)?.preferredMIMEType
                ?? "application/octet-stream"
            listener.onSelected(name: url.lastPathComponent, mimeType: mimeType, bytes: data.kotlinByteArray)
        } catch {
            listener.onError(message: error.localizedDescription)
        }
    }

    func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
        takeFilePickerListener()?.onCancelled()
    }

    private func takeFilePickerListener() -> NativePickedFileListener? {
        let listener = filePickerListener
        filePickerListener = nil
        return listener
    }

    private func topViewController() -> UIViewController? {
        let root = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)?
            .rootViewController
        var current = root
        while let presented = current?.presentedViewController { current = presented }
        return current
    }

    private func workspaceURL() throws -> URL {
        let support = try FileManager.default.url(
            for: .applicationSupportDirectory,
            in: .userDomainMask,
            appropriateFor: nil,
            create: true
        )
        let workspace = support.appendingPathComponent("Workspace", isDirectory: true)
        try FileManager.default.createDirectory(at: workspace, withIntermediateDirectories: true)
        return workspace
    }

    private func chromeRuntimeURL() throws -> URL {
        let support = try FileManager.default.url(
            for: .applicationSupportDirectory,
            in: .userDomainMask,
            appropriateFor: nil,
            create: true
        )
        let runtime = support.appendingPathComponent("ChromiumRuntime", isDirectory: true)
        try FileManager.default.createDirectory(at: runtime, withIntermediateDirectories: true)
        return runtime
    }

    private func chromeDependenciesURL() throws -> URL {
        let support = try FileManager.default.url(
            for: .applicationSupportDirectory,
            in: .userDomainMask,
            appropriateFor: nil,
            create: true
        )
        let dependencies = support.appendingPathComponent("ChromiumDependencies", isDirectory: true)
        try FileManager.default.createDirectory(at: dependencies, withIntermediateDirectories: true)
        return dependencies
    }

    private func installBridgeAsset() throws {
        guard let source = Bundle.main.url(forResource: "bridge", withExtension: "mjs") else {
            throw RuntimeHostError.operationFailed("Bundled Pi Bridge is missing.")
        }
        try guestCreateDirectories("/root/.aether/pi-bridge")
        let bytes = try Data(contentsOf: source)
        try runtime.writeFile(
            "/root/.aether/pi-bridge/bridge.mjs",
            data: bytes,
            executable: false
        )
    }

    private func installNodeCompatibilityAssets() throws {
        for name in ["wasm-polyfill", "fetch-polyfill"] {
            guard let source = Bundle.main.url(forResource: name, withExtension: "js") else {
                throw RuntimeHostError.operationFailed("Bundled Node compatibility asset \(name).js is missing.")
            }
            try runtime.writeFile(
                "/lib/\(name).js",
                data: Data(contentsOf: source),
                executable: false
            )
        }
    }

    private func guestCreateDirectories(_ path: String) throws {
        try runtime.createDirectories(path)
    }

    private func guestBind(hostPath: String, guestPath: String, readOnly: Bool = false) throws {
        try runtime.bindHostPath(hostPath, guestPath: guestPath, readOnly: readOnly)
    }

    private func complete(listener: NativeUnitResultListener, operation: () throws -> Void) {
        do {
            try operation()
            complete { listener.onSuccess() }
        } catch {
            complete { listener.onError(message: error.localizedDescription) }
        }
    }

    private func complete(_ callback: @escaping () -> Void) {
        onMain(callback)
    }

    private func onMain(_ callback: @escaping () -> Void) {
        if Thread.isMainThread {
            callback()
        } else {
            DispatchQueue.main.async(execute: callback)
        }
    }
}

private enum RuntimeHostError: LocalizedError {
    case operationFailed(String)

    var errorDescription: String? {
        switch self {
        case .operationFailed(let message): message
        }
    }
}

private extension Data {
    var kotlinByteArray: KotlinByteArray {
        let result = KotlinByteArray(size: Int32(count))
        for (index, byte) in enumerated() {
            result.set(index: Int32(index), value: Int8(bitPattern: byte))
        }
        return result
    }
}

private extension KotlinByteArray {
    var data: Data {
        var result = Data(count: Int(size))
        result.withUnsafeMutableBytes { buffer in
            guard let baseAddress = buffer.baseAddress?.assumingMemoryBound(to: UInt8.self) else { return }
            for index in 0..<Int(size) {
                baseAddress[index] = UInt8(bitPattern: get(index: Int32(index)))
            }
        }
        return result
    }
}
