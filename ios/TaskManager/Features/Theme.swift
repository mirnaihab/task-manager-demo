import SwiftUI

/// Tokens from DESIGN.md. Colors live in Assets.xcassets (light and dark).
enum Theme {
    enum Spacing {
        static let xs: CGFloat = 4
        static let sm: CGFloat = 8
        static let md: CGFloat = 16
        static let lg: CGFloat = 24
        static let xl: CGFloat = 32
    }

    enum Radius {
        static let md: CGFloat = 8
        static let lg: CGFloat = 16
    }

    static let minTouchTarget: CGFloat = 44
    static let titleMax = 100
    static let maxContentWidth: CGFloat = 640

    static let primary = Color("AppPrimary")
    static let onPrimary = Color("OnPrimary")
    static let secondary = Color("AppSecondary")
    static let surface = Color("Surface")
    static let surfaceVariant = Color("SurfaceVariant")
    static let onSurface = Color("OnSurface")
    static let onSurfaceVariant = Color("OnSurfaceVariant")
    static let outline = Color("Outline")
    static let outlineStrong = Color("OutlineStrong")
    static let error = Color("Error")
    static let onError = Color("OnError")
    static let errorContainer = Color("ErrorContainer")
    static let success = Color("Success")
    static let warning = Color("Warning")
    static let info = Color("Info")

    static func color(for status: Status) -> Color {
        switch status {
        case .todo: info
        case .inProgress: warning
        case .done: success
        }
    }
}
