package repository

import (
	"event_social_platform/internal/models"
	"gorm.io/gorm"
	"time"
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

// Получить все подписки пользователя
func (r *EventSubscriptionRepository) GetUserSubscriptions(userID uint) ([]*models.EventSubscription, error) {
	var subscriptions []*models.EventSubscription
	err := r.db.Preload("Event").Preload("Event.Creator").
		Where("user_id = ?", userID).
		Order("created_at DESC").
		Find(&subscriptions).Error
	return subscriptions, err
}

// Получить предстоящие подписки пользователя
func (r *EventSubscriptionRepository) GetUserUpcomingSubscriptions(userID uint) ([]*models.EventSubscription, error) {
	var subscriptions []*models.EventSubscription
	now := time.Now()
	err := r.db.Preload("Event").Preload("Event.Creator").
		Where("user_id = ? AND events.date_time >= ?", userID, now).
		Joins("JOIN events ON event_subscriptions.event_id = events.id").
		Order("events.date_time ASC").
		Find(&subscriptions).Error
	return subscriptions, err
}

// Получить прошедшие подписки пользователя
func (r *EventSubscriptionRepository) GetUserPastSubscriptions(userID uint) ([]*models.EventSubscription, error) {
	var subscriptions []*models.EventSubscription
	now := time.Now()
	err := r.db.Preload("Event").Preload("Event.Creator").
		Where("user_id = ? AND events.date_time < ?", userID, now).
		Joins("JOIN events ON event_subscriptions.event_id = events.id").
		Order("events.date_time DESC").
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

// Получить количество подписок пользователя
func (r *EventSubscriptionRepository) GetUserSubscriptionsCount(userID uint, count *int64) error {
	return r.db.Model(&models.EventSubscription{}).
		Where("user_id = ?", userID).
		Count(count).Error
}
