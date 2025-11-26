package repository

import (
	"event_social_platform/internal/models"
	"fmt"
	"gorm.io/gorm"
	"strings"
)

type NewsRepository struct {
	db *gorm.DB
}

func NewNewsRepository(db *gorm.DB) *NewsRepository {
	return &NewsRepository{db: db}
}

// Получить ленту новостей для пользователя с пагинацией
func (r *NewsRepository) GetNewsFeed(userID uint, limit int, offset int) ([]*models.WallPost, error) {
	// 1. Получаем друзей
	var friendIDs []uint
	err := r.db.Raw(`
        SELECT CASE 
            WHEN user_id = ? THEN friend_id 
            ELSE user_id 
        END as friend_id 
        FROM friendships 
        WHERE (user_id = ? OR friend_id = ?) AND status = 'accepted'
    `, userID, userID, userID).Pluck("friend_id", &friendIDs).Error

	if err != nil {
		return nil, err
	}

	// 2. Получаем подписки
	var subscriptionIDs []uint
	err = r.db.Model(&models.Subscription{}).
		Where("follower_id = ?", userID).
		Pluck("following_id", &subscriptionIDs).Error

	if err != nil {
		return nil, err
	}

	// 3. Объединяем ID (друзья + подписки + сам пользователь)
	allIDs := make(map[uint]bool)
	allIDs[userID] = true

	for _, id := range friendIDs {
		allIDs[id] = true
	}
	for _, id := range subscriptionIDs {
		allIDs[id] = true
	}

	// 4. Преобразуем в слайс
	var finalIDs []uint
	for id := range allIDs {
		finalIDs = append(finalIDs, id)
	}

	// 5. Если нет ни друзей ни подписок, возвращаем пустой результат
	if len(finalIDs) == 1 && finalIDs[0] == userID {
		return []*models.WallPost{}, nil
	}

	// 6. Получаем посты с пагинацией
	var posts []*models.WallPost
	err = r.db.Preload("Author").
		Where("author_id IN (?)", finalIDs).
		Where("deleted_at IS NULL").
		Order("created_at DESC").
		Limit(limit).
		Offset(offset).
		Find(&posts).Error

	return posts, err
}

// Получить ленту событий для пользователя с пагинацией
// Получить ленту событий для пользователя с пагинацией
func (r *NewsRepository) GetEventsFeed(userID uint, limit int, offset int) ([]*models.Event, error) {
	// 1. Получаем друзей
	var friendIDs []uint
	err := r.db.Raw(`
        SELECT CASE 
            WHEN user_id = ? THEN friend_id 
            ELSE user_id 
        END as friend_id 
        FROM friendships 
        WHERE (user_id = ? OR friend_id = ?) AND status = 'accepted'
    `, userID, userID, userID).Pluck("friend_id", &friendIDs).Error

	if err != nil {
		return nil, err
	}

	// 2. Получаем подписки
	var subscriptionIDs []uint
	err = r.db.Model(&models.Subscription{}).
		Where("follower_id = ?", userID).
		Pluck("following_id", &subscriptionIDs).Error

	if err != nil {
		return nil, err
	}

	// 3. Объединяем ID
	allIDs := make(map[uint]bool)
	allIDs[userID] = true

	for _, id := range friendIDs {
		allIDs[id] = true
	}
	for _, id := range subscriptionIDs {
		allIDs[id] = true
	}

	// 4. Преобразуем в слайс
	var finalIDs []uint
	for id := range allIDs {
		finalIDs = append(finalIDs, id)
	}

	// 5. Если нет ни друзей ни подписок, возвращаем пустой результат
	if len(finalIDs) == 1 && finalIDs[0] == userID {
		return []*models.Event{}, nil
	}

	// 6. Получаем события с пагинацией
	var events []*models.Event
	err = r.db.Preload("Creator").
		Where("creator_id IN (?)", finalIDs).
		Where("deleted_at IS NULL").
		Order("created_at DESC").
		Limit(limit).
		Offset(offset).
		Find(&events).Error

	if err != nil {
		return nil, err
	}

	// 7. ФИЛЬТРУЕМ: оставляем только события, к которым пользователь имеет доступ
	var accessibleEvents []*models.Event
	for _, event := range events {
		// Для публичных событий - всегда доступ
		if !event.IsPrivate {
			accessibleEvents = append(accessibleEvents, event)
			continue
		}

		// Для приватных событий проверяем доступ через подписку на событие
		var eventSubscriptionCount int64
		r.db.Model(&models.EventSubscription{}).
			Where("user_id = ? AND event_id = ?", userID, event.ID).
			Count(&eventSubscriptionCount)

		if eventSubscriptionCount > 0 {
			// Пользователь подписан на это приватное событие - показываем
			accessibleEvents = append(accessibleEvents, event)
		}
		// Остальные приватные события не показываем
	}

	return accessibleEvents, nil
}

// Получить общее количество постов для пагинации
func (r *NewsRepository) GetTotalPostsCount(userID uint) (int64, error) {
	// 1. Получаем друзей
	var friendIDs []uint
	err := r.db.Raw(`
        SELECT CASE 
            WHEN user_id = ? THEN friend_id 
            ELSE user_id 
        END as friend_id 
        FROM friendships 
        WHERE (user_id = ? OR friend_id = ?) AND status = 'accepted'
    `, userID, userID, userID).Pluck("friend_id", &friendIDs).Error

	if err != nil {
		return 0, err
	}

	// 2. Получаем подписки
	var subscriptionIDs []uint
	err = r.db.Model(&models.Subscription{}).
		Where("follower_id = ?", userID).
		Pluck("following_id", &subscriptionIDs).Error

	if err != nil {
		return 0, err
	}

	// 3. Объединяем ID
	allIDs := make(map[uint]bool)
	allIDs[userID] = true

	for _, id := range friendIDs {
		allIDs[id] = true
	}
	for _, id := range subscriptionIDs {
		allIDs[id] = true
	}

	// 4. Преобразуем в слайс
	var finalIDs []uint
	for id := range allIDs {
		finalIDs = append(finalIDs, id)
	}

	// 5. Если нет ни друзей ни подписок
	if len(finalIDs) == 1 && finalIDs[0] == userID {
		return 0, nil
	}

	// 6. Считаем общее количество
	var count int64
	err = r.db.Model(&models.WallPost{}).
		Where("author_id IN (?)", finalIDs).
		Where("deleted_at IS NULL").
		Count(&count).Error

	return count, err
}

// Получить общее количество событий для пагинации
// Получить общее количество событий для пагинации
func (r *NewsRepository) GetTotalEventsCount(userID uint) (int64, error) {
	// 1. Получаем друзей
	var friendIDs []uint
	err := r.db.Raw(`
        SELECT CASE 
            WHEN user_id = ? THEN friend_id 
            ELSE user_id 
        END as friend_id 
        FROM friendships 
        WHERE (user_id = ? OR friend_id = ?) AND status = 'accepted'
    `, userID, userID, userID).Pluck("friend_id", &friendIDs).Error

	if err != nil {
		return 0, err
	}

	// 2. Получаем подписки
	var subscriptionIDs []uint
	err = r.db.Model(&models.Subscription{}).
		Where("follower_id = ?", userID).
		Pluck("following_id", &subscriptionIDs).Error

	if err != nil {
		return 0, err
	}

	// 3. Объединяем ID
	allIDs := make(map[uint]bool)
	allIDs[userID] = true

	for _, id := range friendIDs {
		allIDs[id] = true
	}
	for _, id := range subscriptionIDs {
		allIDs[id] = true
	}

	// 4. Преобразуем в слайс
	var finalIDs []uint
	for id := range allIDs {
		finalIDs = append(finalIDs, id)
	}

	// 5. Если нет ни друзей ни подписок
	if len(finalIDs) == 1 && finalIDs[0] == userID {
		return 0, nil
	}

	// 6. Получаем ВСЕ события друзей и подписок
	var allEvents []*models.Event
	err = r.db.Model(&models.Event{}).
		Where("creator_id IN (?)", finalIDs).
		Where("deleted_at IS NULL").
		Find(&allEvents).Error

	if err != nil {
		return 0, err
	}

	// 7. Считаем только те события, к которым пользователь имеет доступ
	var count int64
	for _, event := range allEvents {
		if !event.IsPrivate {
			// Публичные события всегда доступны
			count++
			continue
		}

		// Для приватных событий проверяем подписку
		var eventSubscriptionCount int64
		r.db.Model(&models.EventSubscription{}).
			Where("user_id = ? AND event_id = ?", userID, event.ID).
			Count(&eventSubscriptionCount)

		if eventSubscriptionCount > 0 {
			count++
		}
	}

	return count, nil
}

func (r *NewsRepository) GetPostsFromUsers(userIDs []uint, limit, offset int) ([]*models.WallPost, error) {
	var posts []*models.WallPost

	if len(userIDs) == 0 {
		return posts, nil // Возвращаем пустой список если нет пользователей
	}

	// Создаем placeholders для IN запроса
	placeholders := make([]string, len(userIDs))
	args := make([]interface{}, len(userIDs))
	for i, id := range userIDs {
		placeholders[i] = "?"
		args[i] = id
	}

	query := fmt.Sprintf(`
        SELECT wp.*, u.* 
        FROM wall_posts wp
        JOIN users u ON wp.author_id = u.id
        WHERE wp.author_id IN (%s) 
        ORDER BY wp.created_at DESC 
        LIMIT ? OFFSET ?
    `, strings.Join(placeholders, ","))

	// Добавляем limit и offset в аргументы
	args = append(args, limit, offset)

	err := r.db.Raw(query, args...).
		Preload("Author").
		Find(&posts).Error

	return posts, err
}

// GetEventsFromUsers - получить события от конкретных пользователей
func (r *NewsRepository) GetEventsFromUsers(userIDs []uint, limit, offset int) ([]*models.Event, error) {
	var events []*models.Event

	if len(userIDs) == 0 {
		return events, nil // Возвращаем пустой список если нет пользователей
	}

	// Создаем placeholders для IN запроса
	placeholders := make([]string, len(userIDs))
	args := make([]interface{}, len(userIDs))
	for i, id := range userIDs {
		placeholders[i] = "?"
		args[i] = id
	}

	query := fmt.Sprintf(`
        SELECT e.*, u.* 
        FROM events e
        JOIN users u ON e.creator_id = u.id
        WHERE e.creator_id IN (%s) 
        AND e.is_private = false  -- Только публичные события
        ORDER BY e.created_at DESC 
        LIMIT ? OFFSET ?
    `, strings.Join(placeholders, ","))

	// Добавляем limit и offset в аргументы
	args = append(args, limit, offset)

	err := r.db.Raw(query, args...).
		Preload("Creator").
		Find(&events).Error

	return events, err
}

func (r *NewsRepository) GetDB() *gorm.DB {
	return r.db
}
