-- Create reviews table for property and landlord reviews
CREATE TABLE IF NOT EXISTS reviews (
    review_id SERIAL PRIMARY KEY,
    booking_id INTEGER NOT NULL,
    property_id INTEGER NOT NULL,
    student_id INTEGER NOT NULL,
    landlord_id INTEGER NOT NULL,
    property_rating INTEGER NOT NULL CHECK (property_rating >= 1 AND property_rating <= 5),
    landlord_rating INTEGER NOT NULL CHECK (landlord_rating >= 1 AND landlord_rating <= 5),
    comment TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraints
    CONSTRAINT fk_booking FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE,
    CONSTRAINT fk_property FOREIGN KEY (property_id) REFERENCES properties(property_id) ON DELETE CASCADE,
    CONSTRAINT fk_student FOREIGN KEY (student_id) REFERENCES studentprofiles(student_id) ON DELETE CASCADE,
    CONSTRAINT fk_landlord FOREIGN KEY (landlord_id) REFERENCES landlordprofiles(landlord_id) ON DELETE CASCADE,

    -- Ensure one review per booking
    CONSTRAINT unique_booking_review UNIQUE (booking_id)
);

-- Create indexes for better query performance
CREATE INDEX idx_reviews_property ON reviews(property_id);
CREATE INDEX idx_reviews_landlord ON reviews(landlord_id);
CREATE INDEX idx_reviews_student ON reviews(student_id);
CREATE INDEX idx_reviews_booking ON reviews(booking_id);
CREATE INDEX idx_reviews_created_at ON reviews(created_at);

-- Enable Row Level Security
ALTER TABLE reviews ENABLE ROW LEVEL SECURITY;

-- RLS Policies

-- Students can insert their own reviews
CREATE POLICY "Students can create reviews for their bookings"
ON reviews FOR INSERT
WITH CHECK (
    student_id IN (
        SELECT student_id
        FROM studentprofiles
        WHERE user_id = auth.uid()
    )
);

-- Students can view their own reviews
CREATE POLICY "Students can view their own reviews"
ON reviews FOR SELECT
USING (
    student_id IN (
        SELECT student_id
        FROM studentprofiles
        WHERE user_id = auth.uid()
    )
);

-- Landlords can view reviews for their properties
CREATE POLICY "Landlords can view reviews for their properties"
ON reviews FOR SELECT
USING (
    landlord_id IN (
        SELECT landlord_id
        FROM landlordprofiles
        WHERE user_id = auth.uid()
    )
);

-- Anyone can view reviews for properties (public)
CREATE POLICY "Public can view all reviews"
ON reviews FOR SELECT
USING (true);

-- Admins can do everything
CREATE POLICY "Admins can manage all reviews"
ON reviews FOR ALL
USING (
    EXISTS (
        SELECT 1 FROM users
        WHERE users.user_id = auth.uid()
        AND users.role = 'admin'
    )
);

-- Add comment for documentation
COMMENT ON TABLE reviews IS 'Stores student reviews for properties and landlords after completed bookings';
COMMENT ON COLUMN reviews.property_rating IS 'Rating for the property (1-5 stars)';
COMMENT ON COLUMN reviews.landlord_rating IS 'Rating for the landlord (1-5 stars)';
COMMENT ON COLUMN reviews.comment IS 'Optional text review/comment from student';
