import SwiftUI

struct ToastView: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.system(size: 15))
            .foregroundStyle(Theme.surface)
            .padding(.horizontal, Theme.Spacing.md)
            .padding(.vertical, Theme.Spacing.sm + Theme.Spacing.xs)
            .background(Theme.onSurface, in: RoundedRectangle(cornerRadius: Theme.Radius.md))
            .accessibilityIdentifier("toast")
    }
}
