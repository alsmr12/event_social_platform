package repository

import (
	"event_social_platform/internal/models"
	"fmt"
	"gorm.io/gorm"
	"time"
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

// GetEventByInviteCode получает событие по коду приглашения
func (r *EventRepository) GetEventByInviteCode(code string) (*models.Event, error) {
	var event models.Event
	err := r.db.Preload("Creator").Where("invite_code = ?", code).First(&event).Error
	if err != nil {
		return nil, err
	}
	return &event, nil
}

// CanUserAccessEvent проверяет, имеет ли пользователь доступ к событию
func (r *EventRepository) CanUserAccessEvent(userID, eventID uint) (bool, error) {
    var event models.Event
    err := r.db.Where("id = ?", eventID).First(&event).Error
    if err != nil {
        return false, err
    }

    // Если событие публичное - доступ есть у всех авторизованных пользователей
    if !event.IsPrivate {
        return true, nil
    }

    // Для приватных событий проверяем различные уровни доступа
    
    // 1. Создатель события всегда имеет доступ
    if event.CreatorID == userID {
        return true, nil
    }

    // 2. Проверяем дружбу с создателем
    var friendshipCount int64
    err = r.db.Model(&models.Friendship{}).
        Where("(user_id = ? AND friend_id = ? AND status = 'accepted') OR (user_id = ? AND friend_id = ? AND status = 'accepted')",
            userID, event.CreatorID, event.CreatorID, userID).
        Count(&friendshipCount).Error
    if err != nil {
        return false, err
    }
    if friendshipCount > 0 {
        return true, nil
    }

    // 3. Проверяем подписку на создателя
    var subscriptionCount int64
    err = r.db.Model(&models.Subscription{}).
        Where("subscriber_id = ? AND target_id = ?", userID, event.CreatorID).
        Count(&subscriptionCount).Error
    if err != nil {
        return false, err
    }
    if subscriptionCount > 0 {
        return true, nil
    }

    // 4. Проверяем подписку на само событие ← ДОБАВЛЯЕМ ЭТУ ПРОВЕРКУ
    var eventSubscriptionCount int64
    err = r.db.Model(&models.EventSubscription{}).
        Where("user_id = ? AND event_id = ?", userID, eventID).
        Count(&eventSubscriptionCount).Error
    if err != nil {
        return false, err
    }
    if eventSubscriptionCount > 0 {
        return true, nil
    }

    // 5. Если ничего не подошло - доступа нет
    return false, nil
}

func (r *EventRepository) GetAllEvents() ([]*models.Event, error) {
	var events []*models.Event
	err := r.db.Preload("Creator").Order("date_time ASC").Find(&events).Error
	if err != nil {
		return nil, err
	}
	return events, nil
}

/// Получить предстоящие события (ВСЕ - и публичные и приватные)
func (r *EventRepository) GetUpcomingEvents() ([]*models.Event, error) {
    var events []*models.Event
    now := time.Now()

    // Получаем ВСЕ события без фильтрации по приватности
    err := r.db.Preload("Creator").
        Where("date_time >= ?", now).
        Order("date_time ASC").
        Find(&events).Error
    if err != nil {
        return nil, err
    }
    return events, nil
}

// Получить прошедшие события (ВСЕ - и публичные и приватные)
func (r *EventRepository) GetPastEvents() ([]*models.Event, error) {
    var events []*models.Event
    now := time.Now()

    // Получаем ВСЕ события без фильтрации по приватности
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