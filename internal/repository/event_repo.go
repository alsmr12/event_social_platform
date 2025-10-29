package repository

import (
	"event_social_platform/internal/models"
	"fmt"
	"gorm.io/gorm"
	"time" // ДОБАВЬТЕ ЭТОТ ИМПОРТ
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
	err := r.db.Preload("Creator").Where("id = ?", id).First(&event).Error
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

// Получить предстоящие события
func (r *EventRepository) GetUpcomingEvents() ([]*models.Event, error) {
	var events []*models.Event
	now := time.Now()
	err := r.db.Preload("Creator").
		Where("date_time >= ?", now).
		Order("date_time ASC").
		Find(&events).Error
	if err != nil {
		return nil, err
	}
	return events, nil
}

// Получить прошедшие события
func (r *EventRepository) GetPastEvents() ([]*models.Event, error) {
	var events []*models.Event
	now := time.Now()
	err := r.db.Preload("Creator").
		Where("date_time < ?", now).
		Order("date_time DESC").
		Find(&events).Error
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
	result := r.db.Where("id = ?", id).Delete(&models.Event{})
	if result.Error != nil {
		return result.Error
	}
	if result.RowsAffected == 0 {
		return fmt.Errorf("event not found")
	}
	return nil
}

func (r *EventRepository) UpdateEvent(event *models.Event) error {
	return r.db.Save(event).Error
}

// Получить количество событий пользователя
func (r *EventRepository) GetUserEventsCount(userID uint, count *int64) error {
	return r.db.Model(&models.Event{}).
		Where("creator_id = ?", userID).
		Count(count).Error
}
