import XCTest

final class AetherUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    func testOnboardingRuntimeAndIOSCapabilitySurface() throws {
        let app = XCUIApplication()
        app.launchArguments += ["-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        app.launch()

        let privacyAgreement = app.buttons["Agree"]
        if privacyAgreement.waitForExistence(timeout: 10) {
            privacyAgreement.tap()
        }
        XCTAssertTrue(app.buttons["Get started"].waitForExistence(timeout: 20))
        XCTAssertTrue(app.staticTexts["Welcome to Aether"].exists)
        app.buttons["Get started"].tap()

        XCTAssertTrue(app.staticTexts["Set up the built-in Alpine Linux environment. It stays inside Aether's private app storage."].waitForExistence(timeout: 20))
        XCTAssertTrue(app.buttons["Initialize"].waitForExistence(timeout: 20))
        app.buttons["Initialize"].tap()
        XCTAssertTrue(app.buttons["Details"].waitForExistence(timeout: 20))
        app.buttons["Details"].tap()
        XCTAssertTrue(app.staticTexts["Setup details"].waitForExistence(timeout: 10))
        app.buttons["Close"].tap()
        XCTAssertTrue(app.buttons["Continue"].waitForExistence(timeout: 300))
        XCTAssertTrue(app.staticTexts["Alpine is ready and will be used as the default local runtime."].exists)
        app.buttons["Continue"].tap()

        XCTAssertTrue(app.buttons["Skip"].waitForExistence(timeout: 30))
        app.buttons["Skip"].tap()
        XCTAssertTrue(app.staticTexts["What can I help with?"].waitForExistence(timeout: 30))

        let composer = app.textViews.firstMatch
        XCTAssertTrue(composer.waitForExistence(timeout: 10))
        composer.tap()
        composer.typeText("keyboard-e2e")
        XCTAssertTrue((composer.value as? String)?.contains("keyboard-e2e") == true)

        if UIDevice.current.userInterfaceIdiom == .pad {
            XCTAssertFalse(app.buttons["Menu"].exists)
            XCTAssertTrue(app.buttons["Settings"].waitForExistence(timeout: 10))
        } else {
            XCTAssertTrue(app.buttons["Menu"].exists)
            app.buttons["Menu"].tap()
            XCTAssertTrue(app.buttons["Settings"].waitForExistence(timeout: 10))
        }
        app.buttons["Settings"].tap()

        XCTAssertTrue(app.staticTexts["General"].waitForExistence(timeout: 15))
        XCTAssertTrue(app.staticTexts["Providers"].exists)
        XCTAssertTrue(app.staticTexts["Personalization"].exists)
        XCTAssertTrue(app.staticTexts["Web tools"].exists)
        XCTAssertTrue(app.staticTexts["Reliability"].exists)
        XCTAssertTrue(app.staticTexts["Skills"].exists)
        XCTAssertTrue(app.staticTexts["Extensions"].exists)
        app.swipeUp()
        XCTAssertTrue(app.staticTexts["MCP Servers"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.staticTexts["Alpine Runtime"].exists)
        app.swipeUp()
        XCTAssertTrue(app.staticTexts["About"].waitForExistence(timeout: 10))
        XCTAssertFalse(app.staticTexts["Termux"].exists)
        XCTAssertFalse(app.staticTexts["Runtime defaults"].exists)
        XCTAssertFalse(app.staticTexts["Agent Mode"].exists)
        XCTAssertFalse(app.staticTexts["Scheduled Tasks"].exists)

        XCUIDevice.shared.orientation = .landscapeLeft
        XCTAssertTrue(app.staticTexts["Settings"].waitForExistence(timeout: 10))
        XCUIDevice.shared.orientation = .portrait
        XCTAssertTrue(app.staticTexts["Settings"].waitForExistence(timeout: 10))

        for _ in 0..<3 where !app.staticTexts["General"].isHittable {
            app.swipeDown()
        }
        XCTAssertTrue(app.staticTexts["General"].isHittable)

        let generalSettings = app.buttons["General, Language, appearance, and app behavior"]
        XCTAssertTrue(generalSettings.waitForExistence(timeout: 10))
        generalSettings.tap()
        XCTAssertTrue(app.staticTexts["Language"].waitForExistence(timeout: 10))
        app.buttons["简体中文"].tap()
        app.buttons["Dark"].tap()
        app.buttons["Save"].tap()

        app.terminate()
        let localizedApp = XCUIApplication()
        localizedApp.launch()

        XCTAssertTrue(localizedApp.staticTexts["想让我帮你做什么？"].waitForExistence(timeout: 30))
        let localizedDarkHome = XCTAttachment(screenshot: localizedApp.screenshot())
        localizedDarkHome.name = "Chinese dark chat home"
        localizedDarkHome.lifetime = .keepAlways
        add(localizedDarkHome)
    }
}
