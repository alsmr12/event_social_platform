package repository

import (
	"event_social_platform/internal/models"
	"gorm.io/gorm"
)

type EventSubscriptionRepository struct {
	db *gorm.DB
}

func NewEventSubscriptionRepository(db *gorm.DB) *EventSubscriptionRepository {
	return &EventSubscriptionRepository{db: db}
}

// Подписаться на событие
func (r *EventSubscriptionRepository) Subscribe(userID, eventID uint) error {
	subscription := &models.EventSubscription{
		UserID:  userID,
		EventID: eventID,
	}
	return r.db.Create(subscription).Error
}

// Отписаться от события
func (r *EventSubscriptionRepository) Unsubscribe(userID, eventID uint) error {
	return r.db.Where("user_id = ? AND event_id = ?", userID, eventID).Delete(&models.EventSubscription{}).Error
}

// Проверить, подписан ли пользователь на событие
func (r *EventSubscriptionRepository) IsSubscribed(userID, eventID uint) (bool, error) {
	var count int64
	err := r.db.Model(&models.EventSubscription{}).
		Where("user_id = ? AND event_id = ?", userID, eventID).
		Count(&count).Error
	return count > 0, err
}

// Получить подписки пользователя
func (r *EventSubscriptionRepository) GetUserSubscriptions(userID uint) ([]*models.EventSubscription, error) {
	var subscriptions []*models.EventSubscription
	err := r.db.Preload("Event").Preload("Event.Creator").
		Where("user_id = ?", userID).
		Order("created_at DESC").
		Find(&subscriptions).Error
	return subscriptions, err
}

// Получить подписчиков события
func (r *EventSubscriptionRepository) GetEventSubscribers(eventID uint) ([]*models.EventSubscription, error) {
	var subscriptions []*models.EventSubscription
	err := r.db.Preload("User").
		Where("event_id = ?", eventID).
		Order("created_at DESC").
		Find(&subscriptions).Error
	return subscriptions, err
}

// Получить количество подписчиков события
func (r *EventSubscriptionRepository) GetSubscribersCount(eventID uint) (int64, error) {
	var count int64
	err := r.db.Model(&models.EventSubscription{}).
		Where("event_id = ?", eventID).
		Count(&count).Error
	return count, err
}