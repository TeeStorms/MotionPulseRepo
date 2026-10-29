# Task List - Complete Migration to XML Layouts & ViewBinding + Compose Removal

- [ ] 1. Migrate Auth & Landing Fragments (`LandingFragment`, `OnboardingFragment`, `LoginFragment`, `RegisterFragment`, `EmailVerificationFragment`) to native XML layouts + ViewBinding
- [ ] 2. Migrate Core Main Tab Fragments (`DashboardFragment`, `MoodFragment`, `HabitsFragment`, `HabitDetailFragment`) to native XML layouts + ViewBinding & Adapters
- [ ] 3. Migrate Community, Stats, Profile & Badges Fragments (`CommunityFragment`, `StatsFragment`, `ProfileFragment`, `BadgesGalleryFragment`) to native XML layouts + ViewBinding
- [ ] 4. Remove `BaseComposeHostFragment` and all Compose UI code / theme files (`Color.kt`, `Theme.kt`, `Type.kt`, `ui/components/*.kt`)
- [ ] 5. Purge all Jetpack Compose dependencies, BOM, compiler plugin, and `buildFeatures { compose = true }` from Gradle
- [ ] 6. Run dependency classpath check and verify zero Compose leakage
- [ ] 7. Run Gradle build `assembleDebug` and Firebase security rules tests
- [ ] 8. Create `walkthrough.artifact.md`
