import Foundation

/// Picks the API at launch. This is the only place that reads `Bundle.main` configuration.
enum AppEnvironment {
    static let mockArgument = "-UITestMock"
    static let seedArgument = "-UITestSeed"

    /// Returns nil when `API_BASE_URL` is missing or invalid and no mock is requested.
    static func makeAPI(
        arguments: [String] = ProcessInfo.processInfo.arguments,
        bundle: Bundle = .main
    ) -> TaskAPI? {
        if arguments.contains(mockArgument) {
            return MockTaskAPI(seed: arguments.contains(seedArgument) ? MockTaskAPI.seeded : [])
        }
        guard let baseURL = baseURL(bundle: bundle) else { return nil }
        return URLSessionTaskAPI(baseURL: baseURL)
    }

    static func baseURL(bundle: Bundle) -> URL? {
        guard let raw = bundle.object(forInfoDictionaryKey: "API_BASE_URL") as? String,
              let url = URL(string: raw), let scheme = url.scheme, ["http", "https"].contains(scheme),
              url.host() != nil else { return nil }
        return url
    }

    static func todayString(now: Date = Date(), calendar: Calendar = .current) -> String {
        DateConversion.string(from: now, calendar: calendar)
    }
}
