package repository

import (
	"event_social_platform/internal/models"
	"gorm.io/gorm"
)

type SubscriptionRepository struct {
	db *gorm.DB
}

func NewSubscriptionRepository(db *gorm.DB) *SubscriptionRepository {
	return &SubscriptionRepository{db: db}
}

// Создать подписку
func (r *SubscriptionRepository) CreateSubscription(subscription *models.Subscription) error {
	return r.db.Create(subscription).Error
}

// Удалить подписку
func (r *SubscriptionRepository) DeleteSubscription(followerID, followingID uint) error {
	return r.db.Where("follower_id = ? AND following_id = ?", followerID, followingID).Delete(&models.Subscription{}).Error
}

// Проверить, подписан ли пользователь
func (r *SubscriptionRepository) IsSubscribed(followerID, followingID uint) (bool, error) {
	var count int64
	err := r.db.Model(&models.Subscription{}).
		Where("follower_id = ? AND following_id = ?", followerID, followingID).
		Count(&count).Error
	return count > 0, err
}

// Получить подписчиков пользователя
func (r *SubscriptionRepository) GetFollowers(userID uint) ([]*models.User, error) {
	var subscriptions []*models.Subscription
	err := r.db.Preload("Follower").
		Where("following_id = ?", userID).
		Find(&subscriptions).Error
	if err != nil {
		return nil, err
	}

	followers := make([]*models.User, len(subscriptions))
	for i, sub := range subscriptions {
		followers[i] = &sub.Follower
	}
	return followers, nil
}

// Получить подписки пользователя (на кого подписан)
func (r *SubscriptionRepository) GetFollowing(userID uint) ([]*models.User, error) {
	var subscriptions []*models.Subscription
	err := r.db.Preload("Following").
		Where("follower_id = ?", userID).
		Find(&subscriptions).Error
	if err != nil {
		return nil, err
	}

	following := make([]*models.User, len(subscriptions))
	for i, sub := range subscriptions {
		following[i] = &sub.Following
	}
	return following, nil
}

// Получить количество подписчиков
func (r *SubscriptionRepository) GetFollowersCount(userID uint) (int64, error) {
	var count int64
	err := r.db.Model(&models.Subscription{}).
		Where("following_id = ?", userID).
		Count(&count).Error
	return count, err
}

// Получить количество подписок
func (r *SubscriptionRepository) GetFollowingCount(userID uint) (int64, error) {
	var count int64
	err := r.db.Model(&models.Subscription{}).
		Where("follower_id = ?", userID).
		Count(&count).Error
	return count, err
}
