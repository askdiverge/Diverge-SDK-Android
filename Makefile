.PHONY: help sync-version check-version android-build android-test android-paparazzi-record install-hooks ci-local

help:
	@echo "Diverge SDK Android make targets:"
	@echo "  make install-hooks            - Enable local pre-commit Git hooks"
	@echo "  make ci-local                 - Run full local Android CI"
	@echo "  make sync-version             - Sync VERSION into docs/README"
	@echo "  make check-version            - Fail if VERSION drifts"
	@echo "  make android-build            - Assemble library + sample"
	@echo "  make android-test             - Unit test + lint + Dokka + release minify + R8 keeps"
	@echo "  make android-paparazzi-record - Record Android UI snapshots"

install-hooks:
	./scripts/install-git-hooks.sh

ci-local:
	./scripts/ci-local.sh

sync-version:
	./scripts/sync-version.sh

check-version:
	./scripts/check-version.sh

android-build:
	./gradlew :diverge-sdk:assemble :sample:assembleDebug

android-test:
	./gradlew :diverge-sdk:test :diverge-sdk:lint :diverge-sdk:dokkaHtml :diverge-sdk:dokkaJavadoc :sample:assembleRelease :sample:verifyR8PublicApiKeeps :sample:testDebugUnitTest

android-paparazzi-record:
	./gradlew :diverge-sdk:recordPaparazziDebug
