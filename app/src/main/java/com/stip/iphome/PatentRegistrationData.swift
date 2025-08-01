//
//  PatentRegistrationData.swift
//  STIP
//
//  Created: 2025-07-05
//

import Foundation

// Structure to handle multiple patent numbers
struct PatentInfo {
    let numbers: [String]
    
    // Format patents for display - either single string or array depending on count
    var displayText: String {
        if numbers.count == 1 {
            return numbers[0]
        } else {
            return numbers.joined(separator: "\n")
        }
    }
    
    // For display in UI components that need simpler format
    var shortDisplayText: String {
        if numbers.count == 1 {
            return numbers[0]
        } else {
            return "\(numbers.count) 특허 등록"
        }
    }
    
    // All patent numbers as an array for copying
    var copyText: String {
        return numbers.joined(separator: "\n")
    }
}

class PatentRegistrationData {
    // Singleton instance for easy access throughout the app
    static let shared = PatentRegistrationData()
    
    // Dictionary to store patent registration info by ticker ID
    private var patentInfo: [String: PatentInfo] = [
        // 기존 티커
        "CDM": PatentInfo(numbers: ["특허 제 10-2621090호"]),
        "IJECT": PatentInfo(numbers: ["특허 제 10-1377987호"]),
        "JWV": PatentInfo(numbers: ["특허 제 10-1912525호"]),
        "KCOT": PatentInfo(numbers: ["특허 제 10-2133229호"]),
        "MDM": PatentInfo(numbers: ["특허 제 10-1753835호"]),
        "SMT": PatentInfo(numbers: ["특허 제 10-6048639호"]),
        "WETALK": PatentInfo(numbers: ["특허 제 10-2004315호", "특허 제 10-2317027호", "특허 제 40-1400891호"]),
        
        // 특허 등록번호가 없는 티커들
        "AXNO": PatentInfo(numbers: []),
        "KATV": PatentInfo(numbers: ["특허 제 10-2536882호"]),
        "SLEEP": PatentInfo(numbers: ["특허 제 10-2762048호", "특허 제 10-2708038호", "특허 제 20-0493828호"]),
        "MSK": PatentInfo(numbers: ["특허 제10-2412492호"])
    ]
    
    private init() {
        // Initialize with empty data
    }
    
    /// Get patent info for a ticker
    /// - Parameter tickerId: The ticker ID
    /// - Returns: PatentInfo if available, nil otherwise
    func getPatentInfo(for tickerId: String) -> PatentInfo? {
        return patentInfo[tickerId]
    }
    
    /// Get formatted patent registration text for a ticker
    /// - Parameter tickerId: The ticker ID
    /// - Returns: Formatted registration number if available, nil otherwise
    func getPatentRegistrationNumber(for tickerId: String) -> String? {
        guard let info = patentInfo[tickerId] else { return nil }
        return info.numbers.isEmpty ? nil : info.displayText
    }
    
    /// Get short display text for patents (used in list views)
    /// - Parameter tickerId: The ticker ID
    /// - Returns: Short display text for patents
    func getShortPatentDisplay(for tickerId: String) -> String? {
        guard let info = patentInfo[tickerId] else { return nil }
        return info.numbers.isEmpty ? nil : info.shortDisplayText
    }
    
    /// Get all patent numbers as an array
    /// - Parameter tickerId: The ticker ID
    /// - Returns: Array of patent numbers
    func getPatentNumbers(for tickerId: String) -> [String] {
        return patentInfo[tickerId]?.numbers ?? []
    }
    
    /// Check if patent registration numbers exist for a ticker
    /// - Parameter tickerId: The ticker ID
    /// - Returns: Boolean indicating if registration numbers exist
    func hasPatentRegistrationNumber(for tickerId: String) -> Bool {
        return patentInfo[tickerId]?.numbers.isEmpty == false
    }
    
    /// Add or update patent registration numbers for a ticker
    /// - Parameters:
    ///   - tickerId: The ticker ID
    ///   - number: The patent registration number
    func setPatentRegistrationNumber(for tickerId: String, number: String) {
        patentInfo[tickerId] = PatentInfo(numbers: [number])
    }
    
    /// Add or update multiple patent registration numbers for a ticker
    /// - Parameters:
    ///   - tickerId: The ticker ID
    ///   - numbers: Array of patent registration numbers
    func setPatentRegistrationNumbers(for tickerId: String, numbers: [String]) {
        patentInfo[tickerId] = PatentInfo(numbers: numbers)
    }
}
