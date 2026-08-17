.PHONY: help sync-version check-version android-build android-test android-paparazzi-record
help:
	@echo "Diverge SDK Android — see README"
sync-version:
	./scripts/sync-version.sh
check-version:
	./scripts/check-version.sh
android-build:
	./gradlew :diverge-sdk:assemble :sample:assembleDebug
android-test:
	./gradlew :diverge-sdk:test :diverge-sdk:lint :sample:assembleRelease :sample:verifyR8PublicApiKeeps
android-paparazzi-record:
	./gradlew :diverge-sdk:recordPaparazziDebug
