#!/bin/bash

# Discover Feature Test Execution Script
# This script runs comprehensive tests for the Discover feature

set -e  # Exit on any error

echo "🧪 Starting Discover Feature Test Suite"
echo "========================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to run tests with error handling
run_test_suite() {
    local test_name="$1"
    local test_command="$2"
    
    print_status "Running $test_name..."
    
    if eval "$test_command"; then
        print_success "$test_name completed successfully"
        return 0
    else
        print_error "$test_name failed"
        return 1
    fi
}

# Initialize counters
total_suites=0
passed_suites=0
failed_suites=0

# Test configuration
TEST_PACKAGE="com.example.health_assistant.features.discover"
COVERAGE_ENABLED=${COVERAGE_ENABLED:-true}
PARALLEL_EXECUTION=${PARALLEL_EXECUTION:-false}
DEVICE_TESTS=${DEVICE_TESTS:-false}

print_status "Test Configuration:"
echo "  - Package: $TEST_PACKAGE"
echo "  - Coverage: $COVERAGE_ENABLED"
echo "  - Parallel: $PARALLEL_EXECUTION"
echo "  - Device Tests: $DEVICE_TESTS"
echo ""

# 1. Unit Tests - Data Layer
echo "📊 Data Layer Tests"
echo "==================="

((total_suites++))
if run_test_suite "DAO Tests" "./gradlew testDebugUnitTest --tests '*DiscoverDaoTest'"; then
    ((passed_suites++))
else
    ((failed_suites++))
fi

((total_suites++))
if run_test_suite "Repository Tests (Basic)" "./gradlew testDebugUnitTest --tests '*DiscoverRepositoryImplTest'"; then
    ((passed_suites++))
else
    ((failed_suites++))
fi

((total_suites++))
if run_test_suite "Repository Tests (Comprehensive)" "./gradlew testDebugUnitTest --tests '*DiscoverRepositoryImplComprehensiveTest'"; then
    ((passed_suites++))
else
    ((failed_suites++))
fi

((total_suites++))
if run_test_suite "Cache Manager Tests" "./gradlew testDebugUnitTest --tests '*ContentCacheManagerTest'"; then
    ((passed_suites++))
else
    ((failed_suites++))
fi

((total_suites++))
if run_test_suite "Entity Tests" "./gradlew testDebugUnitTest --tests '*DiscoverEntitiesTest'"; then
    ((passed_suites++))
else
    ((failed_suites++))
fi

echo ""

# 2. Unit Tests - Domain Layer
echo "🏗️  Domain Layer Tests"
echo "======================"

((total_suites++))
if run_test_suite "Discover Manager Tests" "./gradlew testDebugUnitTest --tests '*DiscoverManagerTest'"; then
    ((passed_suites++))
else
    ((failed_suites++))
fi

((total_suites++))
if run_test_suite "Use Case Tests" "./gradlew testDebugUnitTest --tests '*SimpleUseCasesTest'"; then
    ((passed_suites++))
else
    ((failed_suites++))
fi

((total_suites++))
if run_test_suite "Content Validation Tests" "./gradlew testDebugUnitTest --tests '*ContentCredibilityValidatorTest'"; then
    ((passed_suites++))
else
    ((failed_suites++))
fi

((total_suites++))
if run_test_suite "Analytics Tests" "./gradlew testDebugUnitTest --tests '*AnalyticsManagerTest'"; then
    ((passed_suites++))
else
    ((failed_suites++))
fi

((total_suites++))
if run_test_suite "Error Handling Tests" "./gradlew testDebugUnitTest --tests '*ErrorMapperTest' --tests '*RetryManagerTest'"; then
    ((passed_suites++))
else
    ((failed_suites++))
fi

echo ""

# 3. Unit Tests - Presentation Layer
echo "🎨 Presentation Layer Tests"
echo "==========================="

((total_suites++))
if run_test_suite "ViewModel Tests" "./gradlew testDebugUnitTest --tests '*DiscoverViewModelTest' --tests '*ArticleReaderViewModelTest' --tests '*VideoPlayerViewModelTest' --tests '*BookmarksViewModelTest'"; then
    ((passed_suites++))
else
    ((failed_suites++))
fi

((total_suites++))
if run_test_suite "Adapter Tests" "./gradlew testDebugUnitTest --tests '*DiscoverContentAdapterTest'"; then
    ((passed_suites++))
else
    ((failed_suites++))
fi

((total_suites++))
if run_test_suite "Utility Tests" "./gradlew testDebugUnitTest --tests '*DiscoverContentUtilsTest'"; then
    ((passed_suites++))
else
    ((failed_suites++))
fi

((total_suites++))
if run_test_suite "Search Tests" "./gradlew testDebugUnitTest --tests '*DiscoverSearchFunctionalityTest'"; then
    ((passed_suites++))
else
    ((failed_suites++))
fi

((total_suites++))
if run_test_suite "Navigation Tests" "./gradlew testDebugUnitTest --tests '*DiscoverNavigationHelperTest' --tests '*DiscoverDeepLinkHandlerTest'"; then
    ((passed_suites++))
else
    ((failed_suites++))
fi

echo ""

# 4. Integration Tests
echo "🔗 Integration Tests"
echo "===================="

((total_suites++))
if run_test_suite "Firebase Sync Integration" "./gradlew testDebugUnitTest --tests '*DiscoverFirebaseSyncIntegrationTest'"; then
    ((passed_suites++))
else
    ((failed_suites++))
fi

((total_suites++))
if run_test_suite "Database Integration" "./gradlew testDebugUnitTest --tests '*DiscoverDatabaseIntegrationTest'"; then
    ((passed_suites++))
else
    ((failed_suites++))
fi

((total_suites++))
if run_test_suite "Content Validation Integration" "./gradlew testDebugUnitTest --tests '*ContentCredibilityIntegrationTest'"; then
    ((passed_suites++))
else
    ((failed_suites++))
fi

((total_suites++))
if run_test_suite "Navigation Integration" "./gradlew testDebugUnitTest --tests '*DiscoverNavigationIntegrationTest'"; then
    ((passed_suites++))
else
    ((failed_suites++))
fi

echo ""

# 5. Worker Tests
echo "⚙️  Background Worker Tests"
echo "==========================="

((total_suites++))
if run_test_suite "Content Sync Worker" "./gradlew testDebugUnitTest --tests '*ContentSyncWorkerTest'"; then
    ((passed_suites++))
else
    ((failed_suites++))
fi

((total_suites++))
if run_test_suite "Sync Scheduler Tests" "./gradlew testDebugUnitTest --tests '*ContentSyncSchedulerTest'"; then
    ((passed_suites++))
else
    ((failed_suites++))
fi

((total_suites++))
if run_test_suite "Sync Status Manager" "./gradlew testDebugUnitTest --tests '*SyncStatusManagerTest'"; then
    ((passed_suites++))
else
    ((failed_suites++))
fi

echo ""

# 6. UI Tests (if enabled)
if [ "$DEVICE_TESTS" = true ]; then
    echo "📱 UI Tests (Device Required)"
    echo "============================="
    
    # Check if device is connected
    if adb devices | grep -q "device$"; then
        print_status "Device detected, running UI tests..."
        
        ((total_suites++))
        if run_test_suite "Content Loading UI Tests" "./gradlew connectedAndroidTest --tests '*DiscoverContentLoadingUITest'"; then
            ((passed_suites++))
        else
            ((failed_suites++))
        fi
        
        ((total_suites++))
        if run_test_suite "Bookmarking UI Tests" "./gradlew connectedAndroidTest --tests '*DiscoverBookmarkingUITest'"; then
            ((passed_suites++))
        else
            ((failed_suites++))
        fi
        
        ((total_suites++))
        if run_test_suite "Search UI Tests" "./gradlew connectedAndroidTest --tests '*DiscoverSearchUITest'"; then
            ((passed_suites++))
        else
            ((failed_suites++))
        fi
    else
        print_warning "No device connected, skipping UI tests"
        print_warning "Connect a device or emulator and run with DEVICE_TESTS=true"
    fi
    
    echo ""
fi

# 7. Generate Coverage Report (if enabled)
if [ "$COVERAGE_ENABLED" = true ]; then
    echo "📈 Generating Coverage Report"
    echo "============================="
    
    print_status "Generating Jacoco coverage report..."
    if ./gradlew jacocoTestReport; then
        print_success "Coverage report generated successfully"
        print_status "Coverage report location: app/build/reports/jacoco/jacocoTestReport/html/index.html"
    else
        print_error "Failed to generate coverage report"
    fi
    
    echo ""
fi

# 8. Test Summary
echo "📋 Test Execution Summary"
echo "========================="
echo "Total Test Suites: $total_suites"
echo "Passed: $passed_suites"
echo "Failed: $failed_suites"

if [ $failed_suites -eq 0 ]; then
    print_success "All test suites passed! 🎉"
    echo ""
    echo "✅ The Discover feature is ready for deployment"
    exit 0
else
    print_error "$failed_suites test suite(s) failed"
    echo ""
    echo "❌ Please fix failing tests before deployment"
    
    # Show failed test details
    echo ""
    echo "🔍 Troubleshooting Tips:"
    echo "- Check test logs in app/build/reports/tests/"
    echo "- Run individual failing tests for detailed output"
    echo "- Verify mock setup and test data"
    echo "- Check for timing issues in async tests"
    
    exit 1
fi

# Additional utility functions for development
show_help() {
    echo "Discover Feature Test Runner"
    echo ""
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  --coverage          Enable coverage reporting (default: true)"
    echo "  --no-coverage       Disable coverage reporting"
    echo "  --device-tests      Run UI tests on connected device"
    echo "  --parallel          Enable parallel test execution"
    echo "  --help              Show this help message"
    echo ""
    echo "Environment Variables:"
    echo "  COVERAGE_ENABLED    Enable/disable coverage (true/false)"
    echo "  DEVICE_TESTS        Enable/disable device tests (true/false)"
    echo "  PARALLEL_EXECUTION  Enable/disable parallel execution (true/false)"
    echo ""
    echo "Examples:"
    echo "  $0                          # Run all unit tests with coverage"
    echo "  $0 --device-tests           # Run all tests including UI tests"
    echo "  $0 --no-coverage            # Run tests without coverage"
    echo "  DEVICE_TESTS=true $0        # Run with device tests via env var"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --coverage)
            COVERAGE_ENABLED=true
            shift
            ;;
        --no-coverage)
            COVERAGE_ENABLED=false
            shift
            ;;
        --device-tests)
            DEVICE_TESTS=true
            shift
            ;;
        --parallel)
            PARALLEL_EXECUTION=true
            shift
            ;;
        --help)
            show_help
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done