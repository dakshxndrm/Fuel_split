# 🚀 FuelSplit - Implementation Guide

## Quick Start

### Prerequisites
- Android Studio (Latest version)
- Java 11 or higher
- Android SDK 36
- Gradle 9.3.1

### Setup Steps

1. **Clone/Open Project**
   ```bash
   cd Fuel_split
   ```

2. **Build Project**
   ```bash
   ./gradlew clean build
   ```

3. **Run on Device/Emulator**
   ```bash
   ./gradlew installDebug
   ```

---

## 📋 Detailed Feature Documentation

### Feature 1: Adding a Trip

**User Flow:**
```
1. Click "+ New Trip" FAB
2. Step 1: Enter trip name (required)
3. Step 1: Add members (minimum 2 required)
4. Step 2: Enter distance, mileage, fuel price
5. Step 3: Select who paid
6. Step 3: Choose split method
7. Step 3: Fill in split details (equal/percentage/km)
8. Tap "Submit Trip ✓"
```

**Code Flow:**
- `MainActivity.fabAddTrip` → Starts `AddTripActivity`
- `AddTripActivity.handleNext()` → Validates input → ViewFlipper transitions
- `AddTripActivity.submitTrip()` → Creates Trip object → Returns via Intent
- `MainActivity.onActivityResult()` → Saves to SharedPreferences via Gson

**Input Validation:**
```java
// Step 1 Validation
✓ Trip name not empty
✓ Minimum 2 members

// Step 2 Validation
✓ Distance > 0
✓ Mileage > 0
✓ Fuel price > 0

// Step 3 Validation (Percentage mode)
✓ Each percentage between 0-100
✓ Total percentages = 100% (±0.01)

// Step 3 Validation (KM mode)
✓ Each KM ≥ 0
✓ Total KM > 0
```

---

### Feature 2: Split Methods

#### Equal Split
```
totalCost = (distance / mileage) * fuelPrice
perPersonShare = totalCost / numberOfMembers

Example: ₹150 ÷ 3 people = ₹50 per person
```

#### Percentage Split
```
perPersonCost = (personPercentage / 100) * totalCost

Example: 
- Person A: 50% → ₹75
- Person B: 30% → ₹45
- Person C: 20% → ₹30
Total = 100% ✓
```

#### KM-Based Split
```
perPersonCost = (personKM / totalKM) * totalCost

Example:
- Person A: 60 km → (60/150) * ₹150 = ₹60
- Person B: 50 km → (50/150) * ₹150 = ₹50
- Person C: 40 km → (40/150) * ₹150 = ₹40
```

---

### Feature 3: Settlement Calculation

**Algorithm Overview:**
```
Step 1: Calculate Net Balance
  For each trip:
    - Add amount person paid to their balance
    - Subtract their shares from their balance

Step 2: Categorize
  - Creditors: balance > 0.5 (owed money)
  - Debtors: balance < -0.5 (owes money)

Step 3: Simplify
  - Match debtors with creditors
  - Calculate minimum transactions
  - Format as: "A pays ₹X to B"
```

**Code Implementation:**
```java
// calculateSettlements() → Creates net balance map
HashMap<String, Double> netBalance = new HashMap<>();

// simplifyDebts() → Matches creditors & debtors
// Returns: List<String> with simplified transactions
```

**Example Calculation:**
```
Trip 1: ₹100, Paid by Amit, Equal split (Amit, Rahul, Priya)
- Amit: +100 - 33.33 = +66.67
- Rahul: 0 - 33.33 = -33.33
- Priya: 0 - 33.33 = -33.33

Trip 2: ₹60, Paid by Rahul, Equal split (Amit, Rahul, Priya)
- Amit: -20
- Rahul: +60 - 20 = +40
- Priya: -20

Net Totals:
- Amit: +66.67 - 20 = +46.67 (creditor)
- Rahul: -33.33 + 40 = +6.67 (creditor)
- Priya: -33.33 - 20 = -53.33 (debtor)

Final Settlements:
- Priya pays ₹46.67 to Amit
- Priya pays ₹6.67 to Rahul
```

---

### Feature 4: Data Persistence

**Storage Method:** SharedPreferences + Gson

**Save Operation:**
```java
getSharedPreferences("FuelSplitPrefs", MODE_PRIVATE)
    .edit()
    .putString("list", new Gson().toJson(tripList))
    .apply();
```

**Load Operation:**
```java
String json = getSharedPreferences("FuelSplitPrefs", MODE_PRIVATE)
    .getString("list", null);
tripList = new Gson().fromJson(json, 
    new TypeToken<ArrayList<Trip>>(){}.getType());
```

**Trigger Points:**
- ✅ After adding new trip
- ✅ After deleting trip
- ✅ On app exit (automatic)

---

## 🎨 UI Component Breakdown

### MainActivity Layout Components

```
RelativeLayout (Root)
├── Header (AppHeader)
│   ├── Title: "FuelSplit ⛽"
│   └── Subtitle: Trip count display
├── Content Frame
│   ├── Trips Tab (LinearLayout)
│   │   ├── Empty state (when no trips)
│   │   └── RecyclerView (trip list)
│   └── Balances Tab (LinearLayout)
│       ├── Header section
│       ├── ScrollView
│       └── Settlement cards container
├── Bottom Navigation
└── FAB (Floating Action Button)
```

### AddTripActivity Layout Components

```
LinearLayout (Root - Vertical)
├── Header
│   ├── Step label (Step 1 of 3)
│   └── Step title (Who was there?)
├── LinearProgressIndicator (Progress bar)
├── ViewFlipper (Step container)
│   ├── Step 1: Trip details + Members
│   ├── Step 2: Distance, Mileage, Price
│   └── Step 3: Split method + Details
└── Bottom section
    ├── Divider
    └── Next/Submit button
```

---

## 🔧 Code Architecture

### Package Structure
```
com.example.fuel_split/
├── MainActivity.java (258 lines)
├── AddTripActivity.java (473 lines)
├── Trip.java (28 lines)
└── TripAdapter.java (79 lines)
```

### Key Methods

#### MainActivity
- `onCreate()` - Initialize UI & load data
- `loadData()` - Restore from SharedPreferences
- `saveData()` - Persist to SharedPreferences
- `updateEmptyState()` - Toggle empty state visibility
- `showSettlements()` - Build settlement cards
- `calculateSettlements()` - Calculate net balances
- `simplifyDebts()` - Simplify transaction list
- `onActivityResult()` - Receive trip from AddTripActivity

#### AddTripActivity
- `onCreate()` - Initialize form
- `addMember()` - Add person to trip
- `handleNext()` - Navigate between steps
- `buildStep3()` - Build split UI
- `updateSplitMode()` - Handle split mode selection
- `submitTrip()` - Validate & return trip data
- `findInputRowByIndex()` - Utility for finding input rows

#### Trip (Model)
- Constructor with all fields
- 7 getters for trip properties

#### TripAdapter
- `onCreateViewHolder()` - Create trip card view
- `onBindViewHolder()` - Bind data to view
- `getItemCount()` - Return list size

---

## 🐛 Error Handling

### User Input Validation
```java
// Empty field checks
if (input.isEmpty()) {
    Toast.makeText(context, "Field required", Toast.LENGTH_SHORT).show();
    return;
}

// Number range checks
if (Double.parseDouble(value) <= 0) {
    Toast.makeText(context, "Must be positive", Toast.LENGTH_SHORT).show();
    return;
}

// Percentage sum validation
if (Math.abs(totalPercent - 100) > 0.01) {
    Toast.makeText(context, "Percentages must total 100%", Toast.LENGTH_LONG).show();
    return;
}
```

### Data Loading
```java
// Handle null when loading from SharedPreferences
tripList = new Gson().fromJson(json, 
    new TypeToken<ArrayList<Trip>>(){}.getType());
if (tripList == null) {
    tripList = new ArrayList<>();
}
```

---

## 📊 Resource Reference

### Color Palette
```
Primary Colors:
- bg_primary: #0B0B1A (Dark navy background)
- accent: #00C9A7 (Teal accent)
- money_positive: #00C9A7 (Green for money)
- money_negative: #FF6B6B (Red for debts)

Text Colors:
- text_primary: #F0F0FF (Main text)
- text_secondary: #7B8AA0 (Secondary text)
- text_hint: #3A4A60 (Hint text)

UI Elements:
- bg_card: #1C1C36 (Card background)
- bg_input: #0F0F24 (Input field background)
- border_normal: #22223E (Border color)
```

### String Resources
```
app_name → "FuelSplit"
nav_trips → "Trips"
nav_balances → "Balances"
empty_title → "No trips yet"
empty_subtitle → "Tap + below to log your first ride"
all_settled → "All settled up! ✓"
delete_trip_title → "Delete Trip"
```

### Dimensions
```
Spacing: xs(4dp), sm(8dp), md(16dp), lg(24dp), xl(32dp)
Radii: input_radius(12dp), card_radius(20dp)
Text sizes: title(24sp), body(15sp), caption(13sp), label(12sp)
```

---

## 🧪 Testing Scenarios

### Test Case 1: Basic Trip Addition
```
1. Add trip "Weekend Drive"
2. Add members: Amit, Rahul
3. Distance: 100 km, Mileage: 50 km/L, Price: ₹100/L
4. Total Cost: (100/50) * 100 = ₹200
5. Paid by: Amit
6. Split: Equal → ₹100 each
7. Expected: Rahul pays ₹100 to Amit
```

### Test Case 2: Percentage Split
```
1. Trip: "City Trip", Cost: ₹300
2. Members: A(30%), B(50%), C(20%)
3. Expected: A=₹90, B=₹150, C=₹60
4. Verify percentage validation (must total 100%)
```

### Test Case 3: KM-Based Split
```
1. Trip: "Long Drive", Cost: ₹200
2. Members: A(40km), B(60km)
3. Expected: A=₹80, B=₹120
4. Formula: (km/totalKm) * totalCost
```

### Test Case 4: Multiple Trips Settlement
```
1. Add Trip 1: ₹100, Amit paid
2. Add Trip 2: ₹200, Rahul paid
3. Check settlements simplification
4. Verify net calculations
```

### Test Case 5: Data Persistence
```
1. Add trip
2. Close app
3. Reopen app
4. Verify trip still exists
5. Verify settlement calculations preserved
```

---

## 🚨 Known Limitations & Future Improvements

### Current Limitations
- ❌ Cannot edit trip after creation (delete only)
- ❌ No recurring trip templates
- ❌ No backup/export functionality
- ❌ No search/filter on trips
- ❌ No group management (single list only)

### Recommended Enhancements
1. **Edit Trip Feature** - Allow modification with re-calculation
2. **Trip Categories** - Group by date/destination
3. **Search & Filter** - Find trips by name/date
4. **Statistics** - Show spending trends over time
5. **Export Data** - CSV/PDF export options
6. **Cloud Sync** - Optional Firebase backup
7. **Payment Tracking** - Mark settlements as paid
8. **Notifications** - Remind about pending payments
9. **Multiple Groups** - Separate groups for different friends
10. **Split History** - Show individual split breakdowns

---

## 📱 Deployment

### Build for Release
```bash
# Build signed APK
./gradlew assembleRelease

# Build App Bundle (for Play Store)
./gradlew bundleRelease
```

### Debugging
```bash
# Enable verbose logging
./gradlew build --info

# Run with debug mode
./gradlew installDebug
adb logcat | grep FuelSplit
```

---

## 📞 Support & Troubleshooting

### Common Issues

**Issue:** App crashes on startup
- **Solution:** Clear app data and reinstall
- **Check:** SharedPreferences corruption

**Issue:** Settlement calculations seem wrong
- **Check:** Verify all trip shares sum to total cost
- **Debug:** Log net balance map

**Issue:** UI layout breaking on different screen sizes
- **Solution:** Test on multiple device sizes
- **Check:** ConstraintLayout and weight attributes

---

## ✅ Final Checklist

Before deploying to production:

- ✅ All build errors resolved
- ✅ UI responsive on all screen sizes
- ✅ Data persists correctly
- ✅ Settlement calculations verified
- ✅ Input validation working
- ✅ Tested on multiple Android versions
- ✅ Performance tested (add 50+ trips)
- ✅ Memory leaks checked
- ✅ All strings in resources (no hardcoding)
- ✅ Proper error handling

---

**Version:** 1.0  
**Last Updated:** June 8, 2026  
**Status:** Production Ready ✅


