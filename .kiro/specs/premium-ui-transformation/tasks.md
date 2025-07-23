 # Implementation Plan

## Overview

Simple UI transformation from gradient-heavy design to clean, health-focused green interface. Focus on quick visual improvements without complex testing or production-level optimizations.

## Implementation Tasks

- [x] 1. Update Color System


  - Replace primary colors in colors.xml with health green (#4CAF50)
  - Add light green backgrounds for health cards
  - Keep existing color names for compatibility
  - _Requirements: 1.1, 2.1_

- [x] 2. Fix Typography Styles


  - Update styles.xml to use consistent sans-serif fonts
  - Replace mixed font families with standard system fonts
  - Add green accent text styles
  - _Requirements: 1.2, 1.3_

- [x] 3. Transform Home Fragment


  - Remove premium_gradient_background from fragment_home.xml
  - Change to white background
  - Update health cards with light green backgrounds
  - Remove decorative circle shapes
  - _Requirements: 1.1, 3.1_

- [x] 4. Transform Journal Fragment



  - Remove premium_gradient_background from fragment_journal.xml
  - Change header to white background with dark text
  - Update FAB to green color
  - Remove decorative shapes
  - _Requirements: 1.1, 3.1_

- [x] 5. Transform Discover Fragment



  - Remove premium_gradient_background from fragment_discover.xml
  - Change to white background with dark text
  - Update section headers with green accents
  - Remove decorative shapes
  - _Requirements: 1.1, 3.1_

- [x] 6. Transform Profile Fragment


  - Remove premium_gradient_background from fragment_profile.xml
  - Change to white background
  - Update text colors from white to dark
  - Style buttons with green colors
  - Remove decorative shapes
  - _Requirements: 1.1, 3.1_

- [x] 7. Transform Edit Profile Fragment




  - Remove premium_gradient_background from fragment_edit_profile.xml
  - Replace glassmorphism with clean white cards
  - Update form inputs with green accents
  - Remove decorative shapes
  - _Requirements: 1.1, 3.1_




- [x] 8. Transform Prescriptions Fragment



  - Remove premium_gradient_background from fragment_prescriptions.xml



  - Update toolbar to white background
  - Change FAB to green color
  - Remove decorative shapes


  - _Requirements: 1.1, 3.1_

- [x] 9. Update Dialogs and Bottom Sheets

  - Remove gradients from dialog_prescription_detail.xml


  - Change to white backgrounds
  - Update form styling with green accents
  - _Requirements: 1.1, 3.3_



- [x] 10. Standardize Card Styling

  - Update all card layouts with consistent corner radius (12dp)
  - Apply standard elevation (4dp)
  - Add subtle green accents where appropriate
  - _Requirements: 1.2, 5.1_

- [x] 11. Update Button Styles


  - Change primary button colors to health green
  - Update FAB colors throughout app
  - Style form inputs with green focus states
  - _Requirements: 1.2, 2.1_

- [x] 12. Update Navigation Components

  - Style bottom navigation with green selection states
  - Update search bars with green focus
  - Apply consistent toolbar styling
  - _Requirements: 1.2, 4.1_

## Implementation Notes

### Simple Approach
- Only modify XML layout files and resource files
- No Kotlin code changes required
- No complex testing or optimization needed
- Focus on visual improvements only

### What NOT to Change
- Fragment.kt files
- ViewModel classes
- Database or repository code
- Navigation logic
- Business logic

### Quick Success
- Remove gradient backgrounds → white backgrounds
- Add green color accents for health theme
- Standardize card and button styling
- Keep all functionality working as-is