# Implementation Plan - Multiple Room Management, Room Types, and Initial Payment Configuration

Improve Dormigo to support multiple rooms per boarding house, 6 standardized room types, initial payment configuration (advance, deposit, fees, refund policy), automated calculation, student payment breakdown view, and expanded horizontal room type filters connected to live database records.

## User Review Required

> [!IMPORTANT]
> - **Room Types**: Standardized to **Bedspace, Shared Room, Solo Room, Dormitory Type, Studio Type, Apartment Type**.
> - **Initial Payments**: Configured per room (Monthly Rent, Advance Months, Deposit Months, Other Fees, Refund Policy). Total Move-in Payment is calculated automatically.
> - **Listings Filters**: Horizontal scrollable filter chips covering all 6 room types + All, fully filtering properties based on actual room types available.

## Proposed Changes

### Database & PHP Backend
#### [MODIFY] [boarding_houses.php](file:///C:/xampp/htdocs/Dormigo_Backend/api/boarding_houses.php)
- Update room insertion to support advance payment, security deposit, other move-in fees, descriptions, and refund policies.

#### [MODIFY] [rooms.php](file:///C:/xampp/htdocs/Dormigo_Backend/api/rooms.php)
- Support full CRUD for rooms including payment configuration fields.

### Android Application
#### [MODIFY] [AddBoardingHouseActivity.java](file:///C:/Users/markv/AndroidStudioProjects/DormigoVA/app/src/main/java/com/dormigo/AddBoardingHouseActivity.java) & [activity_add_boarding_house.xml](file:///C:/Users/markv/AndroidStudioProjects/DormigoVA/app/src/main/res/layout/activity_add_boarding_house.xml)
- Implement dynamic **+ Add Another Room** and **Remove Room** functionality.
- Add Dropdown for all 6 room types.
- Add payment configuration fields (Advance Payment options, Security Deposit options, Other Fees, Refund Policy) and auto-calculate Total Move-in Payment.

#### [MODIFY] [ViewBoardingHouseActivity.java](file:///C:/Users/markv/AndroidStudioProjects/DormigoVA/app/src/main/java/com/dormigo/ViewBoardingHouseActivity.java) & [view_boarding_houses.xml](file:///C:/Users/markv/AndroidStudioProjects/DormigoVA/app/src/main/res/layout/view_boarding_houses.xml)
- Display room payment breakdown (Monthly Rent vs Total Move-in Payment, advance, deposit, fees, refund policy).

#### [MODIFY] [BoardingHouseListingsActivity.java](file:///C:/Users/markv/AndroidStudioProjects/DormigoVA/app/src/main/java/com/dormigo/BoardingHouseListingsActivity.java) & [boarding_house_listings.xml](file:///C:/Users/markv/AndroidStudioProjects/DormigoVA/app/src/main/res/layout/boarding_house_listings.xml)
- Expand horizontal filter chips to include all 6 room types + All.
- Connect filters to actual database rooms.

## Verification Plan

### Automated Tests
- Gradle build verification (`:app:assembleDebug`).

### Manual Verification
- Landlord: Add multiple rooms, select room types, configure advance/deposit, verify auto-calculation.
- Student: Browse listings with 6 room type filters, view room payment breakdown.
