---
inclusion: always
---

# BMAD Task Completion Integration for Kiro

## Auto-Sync Task Completion

When working with BMAD stories and tasks, automatically maintain synchronization between individual story completion and the main tasks.md file.

### Integration Points

1. **Story Completion Detection**
   - Monitor when story status changes to "Done" (final completion)
   - Extract task number from story filename (e.g., "1.5" from "1.5.standardize-journal-fragment-with-three-entry-types.md")
   - Update corresponding task in tasks.md from `[ ]` to `[x]`

2. **Automatic Execution**
   - QA agent approval workflow automatically calls update-tasks-status task when setting status to "Done"
   - Manual sync available via `*sync-tasks` command through orchestrator
   - Dev agent does NOT trigger sync (only when QA marks "Done")

3. **File Synchronization Logic**
   ```
   Story File: docs/stories/1.5.standardize-journal-fragment-with-three-entry-types.md
   Task Number: 1.5
   Tasks.md Line: - [ ] 5. Standardize Journal Fragment with Three Entry Types
   Updated Line: - [x] 5. Standardize Journal Fragment with Three Entry Types
   ```

### Error Prevention

- Always backup tasks.md before making changes
- Preserve all existing content and formatting
- Report if task number cannot be parsed or matched
- Handle malformed tasks.md gracefully

### Manual Sync Command

Users can manually sync task completion status:
- `*sync-tasks` - Update tasks.md with all completed story statuses
- Useful for fixing synchronization issues or bulk updates

### Integration with Kiro IDE

This steering file ensures that:
- Task completion is automatically tracked in the main tasks.md file
- No manual intervention required for basic task status updates
- Synchronization happens seamlessly during normal BMAD workflows
- Users can manually trigger sync when needed

### Troubleshooting

If task completion sync fails:
1. Check story filename format matches expected pattern
2. Verify tasks.md file exists and is readable
3. Ensure task numbering in tasks.md matches story numbering
4. Use `*sync-tasks` command to manually trigger update
5. Check for file permission issues or conflicts