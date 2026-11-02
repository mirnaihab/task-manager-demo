import Foundation

/// Converts between the API date text (yyyy-MM-dd) and `Date` for the picker.
/// The text is the source of truth, so no time zone shift reaches the data.
enum DateConversion {
    private static func formatter(calendar: Calendar) -> DateFormatter {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.calendar = calendar
        formatter.timeZone = calendar.timeZone
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter
    }

    static func string(from date: Date, calendar: Calendar = .current) -> String {
        formatter(calendar: calendar).string(from: date)
    }

    static func date(from string: String, calendar: Calendar = .current) -> Date? {
        formatter(calendar: calendar).date(from: string)
    }
}
