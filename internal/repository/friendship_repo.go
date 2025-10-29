package repository

import (
	"event_social_platform/internal/models"
	"gorm.io/gorm"
)

type FriendshipRepository struct {
	db *gorm.DB
}

func NewFriendshipRepository(db *gorm.DB) *FriendshipRepository {
	return &FriendshipRepository{db: db}
}

// Создать запрос на дружбу
func (r *FriendshipRepository) CreateFriendship(friendship *models.Friendship) error {
	return r.db.Create(friendship).Error
}

// Принять запрос на дружбу
func (r *FriendshipRepository) AcceptFriendship(userID, friendID uint) error {
	return r.db.Model(&models.Friendship{}).
		Where("user_id = ? AND friend_id = ? AND status = ?", friendID, userID, models.FriendshipPending).
		Update("status", models.FriendshipAccepted).Error
}

// Отклонить запрос на дружбу
func (r *FriendshipRepository) RejectFriendship(userID, friendID uint) error {
	///return r.db.Model(&models.Friendship{}).
	///Where("user_id = ? AND friend_id = ? AND status = ?", friendID, userID, models.FriendshipPending).
	///Update("status", models.FriendshipRejected).Error
	return r.db.Where("user_id = ? AND friend_id = ? AND status = ?",
		friendID, userID, models.FriendshipPending).
		Delete(&models.Friendship{}).Error
}

// Удалить друга (удаляет обе записи)
func (r *FriendshipRepository) DeleteFriendship(userID, friendID uint) error {
	// Удаляем в обе стороны
	err := r.db.Where("(user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)",
		userID, friendID, friendID, userID).
		Delete(&models.Friendship{}).Error
	return err
}

// Получить статус дружбы между пользователями
func (r *FriendshipRepository) GetFriendshipStatus(userID, friendID uint) (string, error) {
	var friendship models.Friendship
	err := r.db.Where("(user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)",
		userID, friendID, friendID, userID).
		First(&friendship).Error

	if err != nil {
		if err == gorm.ErrRecordNotFound {
			return "none", nil
		}
		return "", err
	}
	return friendship.Status, nil
}

// Получить входящие запросы в друзья
func (r *FriendshipRepository) GetPendingRequests(userID uint) ([]*models.Friendship, error) {
	var requests []*models.Friendship
	err := r.db.Preload("User").
		Where("friend_id = ? AND status = ?", userID, models.FriendshipPending).
		Find(&requests).Error
	return requests, err
}

// Получить исходящие запросы в друзья
func (r *FriendshipRepository) GetSentRequests(userID uint) ([]*models.Friendship, error) {
	var requests []*models.Friendship
	err := r.db.Preload("Friend").
		Where("user_id = ? AND status = ?", userID, models.FriendshipPending).
		Find(&requests).Error
	return requests, err
}

// Получить список друзей (принятые запросы)
func (r *FriendshipRepository) GetFriends(userID uint) ([]*models.User, error) {
	var friendships []*models.Friendship

	// Друзья где userID был инициатором
	err := r.db.Preload("Friend").
		Where("user_id = ? AND status = ?", userID, models.FriendshipAccepted).
		Find(&friendships).Error
	if err != nil {
		return nil, err
	}

	// Друзья где userID был получателем
	var receivedFriendships []*models.Friendship
	err = r.db.Preload("User").
		Where("friend_id = ? AND status = ?", userID, models.FriendshipAccepted).
		Find(&receivedFriendships).Error
	if err != nil {
		return nil, err
	}

	friends := make([]*models.User, 0, len(friendships)+len(receivedFriendships))

	// Добавляем друзей из исходящих запросов
	for _, f := range friendships {
		friends = append(friends, &f.Friend)
	}

	// Добавляем друзей из входящих запросов
	for _, f := range receivedFriendships {
		friends = append(friends, &f.User)
	}

	return friends, nil
}

// Получить количество друзей
func (r *FriendshipRepository) GetFriendsCount(userID uint) (int64, error) {
	// Считаем исходящие принятые запросы
	var outgoingCount int64
	err := r.db.Model(&models.Friendship{}).
		Where("user_id = ? AND status = ?", userID, models.FriendshipAccepted).
		Count(&outgoingCount).Error
	if err != nil {
		return 0, err
	}

	// Считаем входящие принятые запросы
	var incomingCount int64
	err = r.db.Model(&models.Friendship{}).
		Where("friend_id = ? AND status = ?", userID, models.FriendshipAccepted).
		Count(&incomingCount).Error
	if err != nil {
		return 0, err
	}

	return outgoingCount + incomingCount, nil
}

// Проверить, являются ли пользователи друзьями
func (r *FriendshipRepository) AreFriends(userID, friendID uint) (bool, error) {
	var count int64
	err := r.db.Model(&models.Friendship{}).
		Where("((user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)) AND status = ?",
			userID, friendID, friendID, userID, models.FriendshipAccepted).
		Count(&count).Error
	return count > 0, err
}

// Получить количество друзей пользователя
func (r *FriendshipRepository) GetFriendCount(userID uint, count *int64) error {
	return r.db.Model(&models.Friendship{}).
		Where("(user_id = ? OR friend_id = ?) AND status = 'accepted'", userID, userID).
		Count(count).Error
}
