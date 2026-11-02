import SwiftUI

struct FilterChipsView: View {
    let filters: TaskFilters
    let onStatus: (Status?) -> Void
    let onPriority: (Priority?) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.sm) {
            row {
                chip("All", selected: filters.status == nil, id: "chip-status-all") { onStatus(nil) }
                ForEach(Status.allCases, id: \.self) { status in
                    chip(status.label, selected: filters.status == status, id: "chip-status-\(status.rawValue)") {
                        onStatus(status)
                    }
                }
            }
            row {
                chip("All", selected: filters.priority == nil, id: "chip-priority-all") { onPriority(nil) }
                ForEach(Priority.allCases, id: \.self) { priority in
                    chip(priority.label, selected: filters.priority == priority,
                         id: "chip-priority-\(priority.rawValue)") { onPriority(priority) }
                }
            }
        }
    }

    private func row<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Theme.Spacing.sm) { content() }
        }
    }

    private func chip(_ title: String, selected: Bool, id: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: Theme.Spacing.xs) {
                if selected { Image(systemName: "checkmark").font(.caption.weight(.bold)) }
                Text(title).font(.caption.weight(.medium))
            }
            .padding(.horizontal, Theme.Spacing.md)
            .frame(minHeight: 40)
            .foregroundStyle(selected ? Theme.onPrimary : Theme.onSurface)
            .background(selected ? Theme.primary : Theme.surfaceVariant, in: Capsule())
            .overlay(Capsule().stroke(selected ? Theme.primary : Theme.outlineStrong, lineWidth: 1))
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier(id)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}
