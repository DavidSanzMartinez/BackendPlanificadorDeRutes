CREATE TABLE calendar_dates (
                                service_id      VARCHAR(255) NOT NULL,
                                date            DATE NOT NULL,
                                exception_type  INTEGER NOT NULL,
                                PRIMARY KEY (service_id, date)
);

CREATE INDEX idx_calendar_dates_service_id ON calendar_dates (service_id);
CREATE INDEX idx_calendar_dates_date ON calendar_dates (date);