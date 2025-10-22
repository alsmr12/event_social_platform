package repository

import (
	"event_social_platform/internal/models"
	"gorm.io/gorm"
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
        
    return events, err
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

    // 6. Считаем общее количество
    var count int64
    err = r.db.Model(&models.Event{}).
        Where("creator_id IN (?)", finalIDs).
        Where("deleted_at IS NULL").
        Count(&count).Error
        
    return count, err
}