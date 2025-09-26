package repository

import (
	"event_social_platform/internal/models"
	"gorm.io/gorm"
)

type EventRepository struct {
	db *gorm.DB
}

func NewEventRepository(db *gorm.DB) *EventRepository {
	return &EventRepository{db: db}
}

func (r *EventRepository) CreateEvent(event *models.Event) error {
	return r.db.Create(event).Error
}

func (r *EventRepository) GetEventByID(id uint) (*models.Event, error) {
	var event models.Event
	err := r.db.Preload("Creator").First(&event, id).Error
	if err != nil {
		return nil, err
	}
	return &event, nil
}

func (r *EventRepository) GetAllEvents() ([]*models.Event, error) {
	var events []*models.Event
	err := r.db.Preload("Creator").Order("date_time ASC").Find(&events).Error
	if err != nil {
		return nil, err
	}
	return events, nil
}

func (r *EventRepository) GetEventsByType(eventType string) ([]*models.Event, error) {
	var events []*models.Event
	err := r.db.Preload("Creator").Where("type = ?", eventType).Order("date_time ASC").Find(&events).Error
	if err != nil {
		return nil, err
	}
	return events, nil
}

func (r *EventRepository) DeleteEvent(id uint) error {
	return r.db.Delete(&models.Event{}, id).Error
}
