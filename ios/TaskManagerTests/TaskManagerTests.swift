import XCTest
@testable import TaskManager

final class TaskManagerTests: XCTestCase {

    /// REQ-315: the base URL comes from Info.plist.
    func testREQ315_baseURLIsReadFromInfoPlist() throws {
        let url = try XCTUnwrap(AppEnvironment.baseURL(bundle: .main))
        XCTAssertEqual(url.absoluteString, "http://localhost:3000")
    }

    /// REQ-316: the launch argument picks the mock API.
    func testREQ316_mockLaunchArgumentUsesMockAPI() {
        let api = AppEnvironment.makeAPI(arguments: ["-UITestMock"], bundle: .main)
        XCTAssertTrue(api is MockTaskAPI)
    }

    func testREQ315_withoutMockUsesNetworkAPI() {
        let api = AppEnvironment.makeAPI(arguments: [], bundle: .main)
        XCTAssertTrue(api is URLSessionTaskAPI)
    }

    func testDateConversionRoundTripsWithoutShift() throws {
        let date = try XCTUnwrap(DateConversion.date(from: "2026-11-02"))
        XCTAssertEqual(DateConversion.string(from: date), "2026-11-02")
    }
}
