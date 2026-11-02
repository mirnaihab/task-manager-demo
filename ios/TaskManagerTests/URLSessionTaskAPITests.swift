import XCTest
@testable import TaskManager

final class URLSessionTaskAPITests: XCTestCase {

    private var api: URLSessionTaskAPI!

    override func setUp() {
        super.setUp()
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [StubURLProtocol.self]
        api = URLSessionTaskAPI(baseURL: URL(string: "http://stub.test")!,
                                session: URLSession(configuration: configuration))
    }

    override func tearDown() {
        StubURLProtocol.handler = nil
        super.tearDown()
    }

    private func stub(_ status: Int, _ body: String = "") {
        StubURLProtocol.handler = { request in
            let response = HTTPURLResponse(url: request.url!, statusCode: status, httpVersion: nil, headerFields: nil)!
            return (response, Data(body.utf8))
        }
    }

    private let taskJSON = """
    {"id":"1","title":"A","description":null,"priority":"HIGH","status":"TODO",
     "dueDate":"2026-11-10","overdue":false,"createdAt":"x"}
    """

    func testREQ303_listSendsOnlySetFilters() async throws {
        var captured: URL?
        StubURLProtocol.handler = { request in
            captured = request.url
            let response = HTTPURLResponse(url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!
            return (response, Data("[]".utf8))
        }
        _ = try await api.list(filters: TaskFilters(status: .todo, priority: nil))
        XCTAssertEqual(captured?.path, "/api/tasks")
        XCTAssertEqual(captured?.query, "status=TODO")
    }

    func testREQ303_listSendsBothFilters() async throws {
        var query: String?
        StubURLProtocol.handler = { request in
            query = request.url?.query
            let response = HTTPURLResponse(url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!
            return (response, Data("[]".utf8))
        }
        _ = try await api.list(filters: TaskFilters(status: .done, priority: .low))
        XCTAssertEqual(query, "status=DONE&priority=LOW")
    }

    func testStatus200DecodesTaskAndIgnoresUnknownKeys() async throws {
        stub(200, "[\(taskJSON)]")
        let tasks = try await api.list(filters: TaskFilters())
        XCTAssertEqual(tasks.first?.priority, .high)
        XCTAssertNil(tasks.first?.description)
    }

    func testStatus201CreateSendsJSONBody() async throws {
        var method: String?
        var body: [String: Any]?
        StubURLProtocol.handler = { request in
            method = request.httpMethod
            body = request.bodyData().flatMap { try? JSONSerialization.jsonObject(with: $0) as? [String: Any] }
            let response = HTTPURLResponse(url: request.url!, statusCode: 201, httpVersion: nil, headerFields: nil)!
            return (response, Data(self.taskJSON.utf8))
        }
        let created = try await api.create(TaskDraft(title: "A", description: "", priority: .high, dueDate: "2026-11-10"))
        XCTAssertEqual(created.id, "1")
        XCTAssertEqual(method, "POST")
        XCTAssertEqual(body?["title"] as? String, "A")
        XCTAssertEqual(body?["priority"] as? String, "HIGH")
    }

    func testSetStatusSendsOnlyStatus() async throws {
        var body: [String: Any]?
        StubURLProtocol.handler = { request in
            body = request.bodyData().flatMap { try? JSONSerialization.jsonObject(with: $0) as? [String: Any] }
            let response = HTTPURLResponse(url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!
            return (response, Data(self.taskJSON.utf8))
        }
        _ = try await api.setStatus(id: "1", status: .done)
        XCTAssertEqual(body?.keys.sorted(), ["status"])
        XCTAssertEqual(body?["status"] as? String, "DONE")
    }

    func testStatus204DeleteSucceeds() async throws {
        stub(204)
        try await api.delete(id: "1")
    }

    func testSummaryDecodes() async throws {
        stub(200, #"{"total":3,"todo":1,"inProgress":1,"done":1,"overdue":1}"#)
        let summary = try await api.summary()
        XCTAssertEqual(summary, Summary(total: 3, todo: 1, inProgress: 1, done: 1, overdue: 1))
    }

    func testREQ307_status400MapsToValidationWithFields() async {
        stub(400, #"{"error":{"code":"VALIDATION_ERROR","message":"Invalid request","fields":{"title":"Title is required"}}}"#)
        await assertThrows(.validation(fields: ["title": "Title is required"], message: "Invalid request")) {
            _ = try await self.api.create(TaskDraft(title: "", dueDate: "2026-11-10"))
        }
    }

    func testStatus400WithoutEnvelopeMapsToServer() async {
        stub(400, "not json")
        await assertThrows(.server) { _ = try await self.api.summary() }
    }

    func testStatus404MapsToNotFound() async {
        stub(404, #"{"error":{"code":"NOT_FOUND","message":"x"}}"#)
        await assertThrows(.notFound) { try await self.api.delete(id: "1") }
    }

    func testStatus500MapsToServer() async {
        stub(500, "boom")
        await assertThrows(.server) { _ = try await self.api.summary() }
    }

    func testMalformedSuccessBodyMapsToServer() async {
        stub(200, "{}")
        await assertThrows(.server) { _ = try await self.api.summary() }
    }

    func testUnknownEnumValueMapsToServer() async {
        stub(200, #"[{"id":"1","title":"A","description":null,"priority":"URGENT","status":"TODO","dueDate":"2026-11-10","overdue":false}]"#)
        await assertThrows(.server) { _ = try await self.api.list(filters: TaskFilters()) }
    }

    func testREQ314_droppedConnectionMapsToNetwork() async {
        StubURLProtocol.handler = { _ in throw URLError(.notConnectedToInternet) }
        await assertThrows(.network) { _ = try await self.api.summary() }
    }

    private func assertThrows(_ expected: APIError, _ work: () async throws -> Void,
                              file: StaticString = #filePath, line: UInt = #line) async {
        do {
            try await work()
            XCTFail("Expected \(expected)", file: file, line: line)
        } catch {
            XCTAssertEqual(error as? APIError, expected, file: file, line: line)
        }
    }
}

private final class StubURLProtocol: URLProtocol {
    nonisolated(unsafe) static var handler: ((URLRequest) throws -> (HTTPURLResponse, Data))?

    override class func canInit(with request: URLRequest) -> Bool { true }
    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }

    override func startLoading() {
        guard let handler = Self.handler else { return }
        do {
            let (response, data) = try handler(request)
            client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
            client?.urlProtocol(self, didLoad: data)
            client?.urlProtocolDidFinishLoading(self)
        } catch {
            client?.urlProtocol(self, didFailWithError: error)
        }
    }

    override func stopLoading() {}
}

private extension URLRequest {
    /// URLSession turns `httpBody` into a stream before the protocol sees it.
    func bodyData() -> Data? {
        if let httpBody { return httpBody }
        guard let stream = httpBodyStream else { return nil }
        stream.open()
        defer { stream.close() }
        var data = Data()
        var buffer = [UInt8](repeating: 0, count: 1024)
        while stream.hasBytesAvailable {
            let count = stream.read(&buffer, maxLength: buffer.count)
            if count <= 0 { break }
            data.append(buffer, count: count)
        }
        return data
    }
}
