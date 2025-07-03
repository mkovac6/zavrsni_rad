-- Create the database
CREATE DATABASE Accommodation;
GO

USE Accommodation;
GO

-- 1. Users table (for authentication - both students and landlords)
CREATE TABLE Users (
    user_id INT PRIMARY KEY IDENTITY(1,1),
    email NVARCHAR(255) UNIQUE NOT NULL,
    password_hash NVARCHAR(255) NOT NULL,
    user_type NVARCHAR(50) CHECK (user_type IN ('student', 'landlord', 'admin')),
    is_profile_complete BIT DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE()
);

-- 2. Universities lookup table
CREATE TABLE Universities (
    university_id INT PRIMARY KEY IDENTITY(1,1),
    name NVARCHAR(255) NOT NULL,
    city NVARCHAR(100),
    country NVARCHAR(100),
    is_active BIT DEFAULT 1
);

-- 3. Student profiles
CREATE TABLE Students (
    student_id INT PRIMARY KEY IDENTITY(1,1),
    user_id INT UNIQUE FOREIGN KEY REFERENCES Users(user_id),
    university_id INT FOREIGN KEY REFERENCES Universities(university_id),
    first_name NVARCHAR(100) NOT NULL,
    last_name NVARCHAR(100) NOT NULL,
    phone NVARCHAR(20),
    student_number NVARCHAR(50),
    year_of_study INT,
    program NVARCHAR(255),
    profile_picture NVARCHAR(500),
    bio NVARCHAR(MAX),
    preferred_move_in_date DATE,
    budget_min DECIMAL(10,2),
    budget_max DECIMAL(10,2),
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE()
);

-- 4. Landlord profiles
CREATE TABLE Landlords (
    landlord_id INT PRIMARY KEY IDENTITY(1,1),
    user_id INT UNIQUE FOREIGN KEY REFERENCES Users(user_id),
    first_name NVARCHAR(100) NOT NULL,
    last_name NVARCHAR(100) NOT NULL,
    company_name NVARCHAR(255),
    phone NVARCHAR(20) NOT NULL,
    is_verified BIT DEFAULT 0,
    rating DECIMAL(3,2),
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE()
);

-- 5. Properties/Accommodations
CREATE TABLE Properties (
    property_id INT PRIMARY KEY IDENTITY(1,1),
    landlord_id INT FOREIGN KEY REFERENCES Landlords(landlord_id),
    title NVARCHAR(255) NOT NULL,
    description NVARCHAR(MAX),
    property_type NVARCHAR(50) CHECK (property_type IN ('apartment', 'house', 'room', 'studio', 'shared')),
    address NVARCHAR(500) NOT NULL,
    city NVARCHAR(100) NOT NULL,
    postal_code NVARCHAR(20),
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8),
    price_per_month DECIMAL(10,2) NOT NULL,
    bedrooms INT NOT NULL,
    bathrooms INT NOT NULL,
    total_capacity INT NOT NULL,
    available_from DATE NOT NULL,
    available_to DATE,
    is_active BIT DEFAULT 1,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE()
);

-- 6. Property Images
CREATE TABLE PropertyImages (
    image_id INT PRIMARY KEY IDENTITY(1,1),
    property_id INT FOREIGN KEY REFERENCES Properties(property_id),
    image_url NVARCHAR(500) NOT NULL,
    is_primary BIT DEFAULT 0,
    uploaded_at DATETIME DEFAULT GETDATE()
);

-- 7. Amenities lookup table
CREATE TABLE Amenities (
    amenity_id INT PRIMARY KEY IDENTITY(1,1),
    name NVARCHAR(100) NOT NULL UNIQUE,
    category NVARCHAR(50) -- 'essential', 'comfort', 'safety', etc.
);

-- 8. Property Amenities (many-to-many)
CREATE TABLE PropertyAmenities (
    property_id INT FOREIGN KEY REFERENCES Properties(property_id),
    amenity_id INT FOREIGN KEY REFERENCES Amenities(amenity_id),
    PRIMARY KEY (property_id, amenity_id)
);

-- 9. Bookings/Applications
CREATE TABLE Bookings (
    booking_id INT PRIMARY KEY IDENTITY(1,1),
    property_id INT FOREIGN KEY REFERENCES Properties(property_id),
    student_id INT FOREIGN KEY REFERENCES Students(student_id),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status NVARCHAR(50) CHECK (status IN ('pending', 'approved', 'rejected', 'cancelled', 'completed')),
    total_price DECIMAL(10,2),
    message_to_landlord NVARCHAR(MAX),
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE()
);

-- 10. Reviews
CREATE TABLE Reviews (
    review_id INT PRIMARY KEY IDENTITY(1,1),
    property_id INT FOREIGN KEY REFERENCES Properties(property_id),
    student_id INT FOREIGN KEY REFERENCES Students(student_id),
    booking_id INT FOREIGN KEY REFERENCES Bookings(booking_id),
    rating INT CHECK (rating >= 1 AND rating <= 5),
    comment NVARCHAR(MAX),
    created_at DATETIME DEFAULT GETDATE()
);

-- 11. Messages (for communication between students and landlords)
--TBD

-- 12. Favorites (students can save properties)
CREATE TABLE Favorites (
    student_id INT FOREIGN KEY REFERENCES Students(student_id),
    property_id INT FOREIGN KEY REFERENCES Properties(property_id),
    created_at DATETIME DEFAULT GETDATE(),
    PRIMARY KEY (student_id, property_id)
);

-- Samples
INSERT INTO Universities (name, city, country) VALUES 
('University of Zagreb', 'Zagreb', 'Croatia'),
('Zagreb School of Economics and Management', 'Zagreb', 'Croatia'),
('University of Split', 'Split', 'Croatia'),
('University of Rijeka', 'Rijeka', 'Croatia');

INSERT INTO Amenities (name, category) VALUES 
('WiFi', 'essential'),
('Parking', 'essential'),
('Air Conditioning', 'comfort'),
('Heating', 'essential'),
('Washing Machine', 'comfort'),
('Kitchen', 'essential'),
('Furnished', 'comfort'),
('Security System', 'safety'),
('Elevator', 'comfort'),
('Balcony', 'comfort');

-- Insert sample users (remember to hash passwords in your actual app!)
-- For testing, let's use 'password123' as the password for all users

-- Sample Student User
INSERT INTO Users (email, password_hash, user_type, is_profile_complete)
VALUES ('ana.kovac@student.hr', 'password123', 'student', 1);

-- Sample Landlord User
INSERT INTO Users (email, password_hash, user_type, is_profile_complete)
VALUES ('marko.novak@gmail.com', 'password123', 'landlord', 1);

-- Get the user IDs we just created
DECLARE @studentUserId INT = (SELECT user_id FROM Users WHERE email = 'ana.kovac@student.hr');
DECLARE @landlordUserId INT = (SELECT user_id FROM Users WHERE email = 'marko.novak@gmail.com');

-- Create Student Profile
INSERT INTO Students (
    user_id, 
    university_id, 
    first_name, 
    last_name, 
    phone, 
    student_number, 
    year_of_study, 
    program, 
    bio, 
    preferred_move_in_date, 
    budget_min, 
    budget_max
)
VALUES (
    @studentUserId,
    1, -- University of Zagreb
    'Ana',
    'Kovač',
    '+385991234567',
    'STU2024001',
    2,
    'Computer Science',
    'Second year CS student looking for a quiet place to study near campus.',
    '2025-09-01',
    300.00,
    500.00
);

-- Create Landlord Profile
INSERT INTO Landlords (
    user_id,
    first_name,
    last_name,
    company_name,
    phone,
    is_verified,
    rating
)
VALUES (
    @landlordUserId,
    'Marko',
    'Novak',
    'Novak Properties',
    '+385912345678',
    1,
    4.5
);

-- Let's also create a sample property for the landlord
DECLARE @landlordId INT = (SELECT landlord_id FROM Landlords WHERE user_id = @landlordUserId);

INSERT INTO Properties (
    landlord_id,
    title,
    description,
    property_type,
    address,
    city,
    postal_code,
    price_per_month,
    bedrooms,
    bathrooms,
    total_capacity,
    available_from
)
VALUES (
    @landlordId,
    'Modern Studio Apartment Near University',
    'Fully furnished studio apartment, 5 minutes walk from University of Zagreb. Perfect for students!',
    'studio',
    'Ilica 123',
    'Zagreb',
    '10000',
    450.00,
    1,
    1,
    1,
    '2025-08-01'
);

-- Add some amenities to the property
DECLARE @propertyId INT = SCOPE_IDENTITY();

INSERT INTO PropertyAmenities (property_id, amenity_id)
SELECT @propertyId, amenity_id 
FROM Amenities 
WHERE name IN ('WiFi', 'Kitchen', 'Furnished', 'Heating', 'Air Conditioning');

/*SELECT * FROM Users;
SELECT * FROM Students;
SELECT * FROM Landlords;
SELECT * FROM Properties;*/