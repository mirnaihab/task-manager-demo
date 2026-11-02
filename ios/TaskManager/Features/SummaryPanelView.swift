import SwiftUI

struct SummaryPanelView: View {
    let summary: Summary

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.sm) {
            VStack(alignment: .leading, spacing: 0) {
                Text("\(summary.total)")
                    .font(.system(size: 28, weight: .bold))
                Text("Total").font(.caption.weight(.medium))
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("Total")
            .accessibilityValue("\(summary.total)")
            .accessibilityIdentifier("summary-total")
            HStack(alignment: .top) {
                count("To Do", summary.todo, id: "summary-todo")
                count("In Progress", summary.inProgress, id: "summary-in-progress")
                count("Done", summary.done, id: "summary-done")
            }
            Text("Overdue: \(summary.overdue)")
                .font(.caption.weight(.medium))
                .foregroundStyle(summary.overdue > 0 ? Theme.error : Theme.secondary)
                .accessibilityIdentifier("summary-overdue")
        }
        .foregroundStyle(Theme.onSurface)
        .padding(Theme.Spacing.md)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Theme.surfaceVariant, in: RoundedRectangle(cornerRadius: Theme.Radius.lg))
    }

    private func count(_ caption: String, _ value: Int, id: String) -> some View {
        VStack(spacing: 0) {
            Text("\(value)")
                .font(.system(size: 17, weight: .semibold))
            Text(caption).font(.caption.weight(.medium)).multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(caption)
        .accessibilityValue("\(value)")
        .accessibilityIdentifier(id)
    }
}
