package repository

import (
	"event_social_platform/internal/models"
	"gorm.io/gorm"
	"log"
	"sort"
	"time"
)

type AchievementRepository struct {
	db *gorm.DB
}

func NewAchievementRepository(db *gorm.DB) *AchievementRepository {
	return &AchievementRepository{db: db}
}

// Создать достижение
func (r *AchievementRepository) CreateAchievement(achievement *models.Achievement) error {
	return r.db.Create(achievement).Error
}

// Получить все достижения
func (r *AchievementRepository) GetAllAchievements() ([]*models.Achievement, error) {
	var achievements []*models.Achievement
	err := r.db.Order("points DESC").Find(&achievements).Error
	return achievements, err
}

// Получить достижения по типу
func (r *AchievementRepository) GetAchievementsByType(achievementType string) ([]*models.Achievement, error) {
	var achievements []*models.Achievement
	err := r.db.Where("type = ?", achievementType).Find(&achievements).Error
	return achievements, err
}

// Получить достижения пользователя
func (r *AchievementRepository) GetUserAchievements(userID uint) ([]*models.UserAchievement, error) {
	var userAchievements []*models.UserAchievement
	err := r.db.Preload("Achievement").
		Where("user_id = ?", userID).
		Order("completed DESC, created_at DESC").
		Find(&userAchievements).Error
	return userAchievements, err
}

// Получить конкретное достижение пользователя
func (r *AchievementRepository) GetUserAchievement(userID, achievementID uint) (*models.UserAchievement, error) {
	var userAchievement models.UserAchievement
	err := r.db.Preload("Achievement").
		Where("user_id = ? AND achievement_id = ?", userID, achievementID).
		First(&userAchievement).Error
	return &userAchievement, err
}

// Обновить прогресс достижения
func (r *AchievementRepository) UpdateAchievementProgress(userID, achievementID uint, progress int) error {
	var userAchievement models.UserAchievement

	err := r.db.Where("user_id = ? AND achievement_id = ?", userID, achievementID).
		First(&userAchievement).Error

	if err != nil {
		// Создаем новую запись ДАЖЕ ЕСЛИ progress = 0
		userAchievement = models.UserAchievement{
			UserID:        userID,
			AchievementID: achievementID,
			Progress:      progress,
			Completed:     false,
		}

		// Если прогресс достаточен для выполнения - отмечаем как выполненное
		var achievement models.Achievement
		if err := r.db.First(&achievement, achievementID).Error; err == nil {
			if progress >= achievement.Condition {
				userAchievement.Completed = true
				now := time.Now()
				userAchievement.CompletedAt = &now
			}
		}

		return r.db.Create(&userAchievement).Error
	}

	// Обновляем существующую запись
	userAchievement.Progress = progress

	// Проверяем, выполнено ли достижение
	var achievement models.Achievement
	if err := r.db.First(&achievement, achievementID).Error; err != nil {
		return err
	}

	// Обновляем статус выполнения
	if progress >= achievement.Condition && !userAchievement.Completed {
		userAchievement.Completed = true
		now := time.Now()
		userAchievement.CompletedAt = &now
	} else if progress < achievement.Condition && userAchievement.Completed {
		// Если прогресс уменьшился ниже условия, снимаем выполнение
		userAchievement.Completed = false
		userAchievement.CompletedAt = nil
	}

	return r.db.Save(&userAchievement).Error
}

// REAL-TIME МЕТОДЫ ДЛЯ ОБНОВЛЕНИЯ ДОСТИЖЕНИЙ

// UpdateAchievementsOnEventCreated - обновить достижения при создании события
func (r *AchievementRepository) UpdateAchievementsOnEventCreated(userID uint) error {
	achievements, err := r.GetAchievementsByType("event_creator")
	if err != nil {
		return err
	}

	currentProgress, err := r.getEventCreationCount(userID)
	if err != nil {
		return err
	}

	for _, achievement := range achievements {
		if err := r.UpdateAchievementProgress(userID, achievement.ID, currentProgress); err != nil {
			log.Printf("Error updating achievement %d for user %d: %v", achievement.ID, userID, err)
		}
	}
	return nil
}

// UpdateAchievementsOnEventSubscribed - при подписке на событие
func (r *AchievementRepository) UpdateAchievementsOnEventSubscribed(userID uint) error {
	achievements, err := r.GetAchievementsByType("event_subscriber")
	if err != nil {
		return err
	}

	currentProgress, err := r.getEventSubscriptionCount(userID)
	if err != nil {
		return err
	}

	for _, achievement := range achievements {
		if err := r.UpdateAchievementProgress(userID, achievement.ID, currentProgress); err != nil {
			log.Printf("Error updating achievement %d for user %d: %v", achievement.ID, userID, err)
		}
	}
	return nil
}

// UpdateAchievementsOnEventParticipated - при участии в событии (после окончания)
func (r *AchievementRepository) UpdateAchievementsOnEventParticipated(userID uint) error {
	achievements, err := r.GetAchievementsByType("event_participant")
	if err != nil {
		return err
	}

	currentProgress, err := r.getEventParticipationCount(userID)
	if err != nil {
		return err
	}

	for _, achievement := range achievements {
		if err := r.UpdateAchievementProgress(userID, achievement.ID, currentProgress); err != nil {
			log.Printf("Error updating achievement %d for user %d: %v", achievement.ID, userID, err)
		}
	}
	return nil
}

// UpdateAchievementsOnUserSubscribed - при подписке на пользователя
func (r *AchievementRepository) UpdateAchievementsOnUserSubscribed(userID uint) error {
	achievements, err := r.GetAchievementsByType("social_subscription")
	if err != nil {
		return err
	}

	currentProgress, err := r.getUserSubscriptionCount(userID)
	if err != nil {
		return err
	}

	for _, achievement := range achievements {
		if err := r.UpdateAchievementProgress(userID, achievement.ID, currentProgress); err != nil {
			log.Printf("Error updating achievement %d for user %d: %v", achievement.ID, userID, err)
		}
	}
	return nil
}

// UpdateAchievementsOnFriendshipAdded - при добавлении друга
func (r *AchievementRepository) UpdateAchievementsOnFriendshipAdded(userID uint) error {
	achievements, err := r.GetAchievementsByType("friends")
	if err != nil {
		return err
	}

	currentProgress, err := r.getFriendsCount(userID)
	if err != nil {
		return err
	}

	for _, achievement := range achievements {
		if err := r.UpdateAchievementProgress(userID, achievement.ID, currentProgress); err != nil {
			log.Printf("Error updating achievement %d for user %d: %v", achievement.ID, userID, err)
		}
	}
	return nil
}

// Вспомогательные методы для получения текущих счетчиков
func (r *AchievementRepository) getEventCreationCount(userID uint) (int, error) {
	var count int64
	err := r.db.Model(&models.Event{}).Where("creator_id = ?", userID).Count(&count).Error
	return int(count), err
}

func (r *AchievementRepository) getEventSubscriptionCount(userID uint) (int, error) {
	var count int64
	err := r.db.Model(&models.EventSubscription{}).Where("user_id = ?", userID).Count(&count).Error
	return int(count), err
}

func (r *AchievementRepository) getEventParticipationCount(userID uint) (int, error) {
	var count int64
	err := r.db.Model(&models.EventSubscription{}).
		Joins("JOIN events ON event_subscriptions.event_id = events.id").
		Where("event_subscriptions.user_id = ? AND events.date_time < ?", userID, time.Now()).
		Count(&count).Error
	return int(count), err
}

func (r *AchievementRepository) getUserSubscriptionCount(userID uint) (int, error) {
	var count int64
	err := r.db.Model(&models.Subscription{}).Where("follower_id = ?", userID).Count(&count).Error
	return int(count), err
}

func (r *AchievementRepository) getFriendsCount(userID uint) (int, error) {
	var count int64
	err := r.db.Model(&models.Friendship{}).
		Where("(user_id = ? OR friend_id = ?) AND status = ?", userID, userID, "accepted").
		Count(&count).Error
	return int(count), err
}

// Получить рейтинг пользователей
func (r *AchievementRepository) GetUserRatings(search string) ([]map[string]interface{}, error) {
	var users []models.User
	query := r.db.Model(&models.User{})

	if search != "" {
		query = query.Where("first_name LIKE ? OR last_name LIKE ?",
			"%"+search+"%", "%"+search+"%")
	}

	err := query.Find(&users).Error
	if err != nil {
		return nil, err
	}

	var ratings []map[string]interface{}
	for _, user := range users {
		points, _ := r.GetUserTotalPoints(user.ID)
		ratings = append(ratings, map[string]interface{}{
			"user_id":    user.ID,
			"first_name": user.FirstName,
			"last_name":  user.LastName,
			"points":     points,
		})
	}

	// Сортируем по убыванию очков
	sort.Slice(ratings, func(i, j int) bool {
		return ratings[i]["points"].(int) > ratings[j]["points"].(int)
	})

	return ratings, nil
}

// Получить общее количество очков пользователя
func (r *AchievementRepository) GetUserTotalPoints(userID uint) (int, error) {
	var userAchievements []models.UserAchievement
	err := r.db.Preload("Achievement").
		Where("user_id = ? AND completed = ?", userID, true).
		Find(&userAchievements).Error

	if err != nil {
		return 0, err
	}

	totalPoints := 0
	for _, ua := range userAchievements {
		totalPoints += ua.Achievement.Points
	}

	return totalPoints, nil
}

// Обновить прогресс всех пользователей
func (r *AchievementRepository) UpdateAllUsersProgress() error {
	// Сначала проверяем что достижения существуют
	var achievementsCount int64
	if err := r.db.Model(&models.Achievement{}).Count(&achievementsCount).Error; err != nil {
		return err
	}

	if achievementsCount == 0 {
		log.Println("No achievements found, skipping progress update")
		return nil
	}

	log.Printf("Found %d achievements, updating progress...", achievementsCount)

	// Получаем всех пользователей
	var users []models.User
	if err := r.db.Find(&users).Error; err != nil {
		return err
	}

	// Получаем все достижения
	var achievements []models.Achievement
	if err := r.db.Find(&achievements).Error; err != nil {
		return err
	}

	log.Printf("Processing %d users with %d achievements", len(users), len(achievements))

	// Для каждого пользователя обновляем прогресс
	for _, user := range users {
		// Получаем счетчики
		var participatedEventsCount, eventsCreatedCount, userSubscriptionsCount, friendsCount int64

		// 1. ПРОШЕДШИЕ СОБЫТИЯ, В КОТОРЫХ ПОЛЬЗОВАТЕЛЬ УЧАСТВОВАЛ
		// Подписки на ПРОШЕДШИЕ события
		r.db.Model(&models.EventSubscription{}).
			Joins("JOIN events ON event_subscriptions.event_id = events.id").
			Where("event_subscriptions.user_id = ? AND events.date_time < ?", user.ID, time.Now()).
			Count(&participatedEventsCount)

		// 2. СОЗДАННЫЕ СОБЫТИЯ
		r.db.Model(&models.Event{}).Where("creator_id = ?", user.ID).Count(&eventsCreatedCount)

		// 3. ПОДПИСКИ НА ПОЛЬЗОВАТЕЛЕЙ
		r.db.Model(&models.Subscription{}).Where("follower_id = ?", user.ID).Count(&userSubscriptionsCount)

		// 4. ДРУЗЬЯ
		r.db.Model(&models.Friendship{}).Where("(user_id = ? OR friend_id = ?) AND status = ?", user.ID, user.ID, "accepted").Count(&friendsCount)

		log.Printf("User %d: participated_events=%d, events_created=%d, user_subs=%d, friends=%d",
			user.ID, participatedEventsCount, eventsCreatedCount, userSubscriptionsCount, friendsCount)

		// Обновляем ВСЕ достижения с правильными счетчиками
		for _, achievement := range achievements {
			var progress int

			switch achievement.Type {
			case "event_participant":
				// Участие в ПРОШЕДШИХ событиях - используем participatedEventsCount
				progress = int(participatedEventsCount)
			case "event_subscriber":
				// Подписки на события (все) - используем старый счетчик
				var eventSubscriptionsCount int64
				r.db.Model(&models.EventSubscription{}).Where("user_id = ?", user.ID).Count(&eventSubscriptionsCount)
				progress = int(eventSubscriptionsCount)
			case "event_creator":
				// Создание событий - используем eventsCreatedCount
				progress = int(eventsCreatedCount)
			case "social_subscription":
				// Подписки на пользователей - используем userSubscriptionsCount
				progress = int(userSubscriptionsCount)
			case "friends":
				// Друзья - используем friendsCount
				progress = int(friendsCount)
			default:
				progress = 0
			}

			if err := r.UpdateAchievementProgress(user.ID, achievement.ID, progress); err != nil {
				log.Printf("Failed to update achievement %d for user %d: %v", achievement.ID, user.ID, err)
			} else {
				log.Printf("Updated achievement %s for user %d: progress=%d/%d",
					achievement.Name, user.ID, progress, achievement.Condition)
			}
		}
	}

	log.Printf("Updated progress for %d users", len(users))
	return nil
}

// Очистить таблицы наград
func (r *AchievementRepository) ClearAchievements() error {
	// Удаляем данные из таблиц (в правильном порядке из-за foreign keys)
	if err := r.db.Where("1 = 1").Delete(&models.UserAchievement{}).Error; err != nil {
		return err
	}
	if err := r.db.Where("1 = 1").Delete(&models.Achievement{}).Error; err != nil {
		return err
	}

	return nil
}

// Инициализировать базовые достижения
func (r *AchievementRepository) InitializeAchievements() error {
	achievements := []models.Achievement{
		// Участие в событиях
		{
			Name:        "Первый шаг",
			Description: "Принять участие в 1 событии",
			Icon:        "🎯",
			Points:      5,
			Type:        "event_participant",
			Condition:   1,
		},
		{
			Name:        "Активный участник",
			Description: "Принять участие в 3 событиях",
			Icon:        "🏆",
			Points:      15,
			Type:        "event_participant",
			Condition:   3,
		},
		{
			Name:        "Постоянный гость",
			Description: "Принять участие в 5 событиях",
			Icon:        "⭐",
			Points:      25,
			Type:        "event_participant",
			Condition:   5,
		},
		{
			Name:        "Мастер событий",
			Description: "Принять участие в 10 событиях",
			Icon:        "👑",
			Points:      50,
			Type:        "event_participant",
			Condition:   10,
		},

		// Создание событий
		{
			Name:        "Первый организатор",
			Description: "Создать 1 событие",
			Icon:        "🎪",
			Points:      10,
			Type:        "event_creator",
			Condition:   1,
		},
		{
			Name:        "Опытный организатор",
			Description: "Создать 3 события",
			Icon:        "🎭",
			Points:      30,
			Type:        "event_creator",
			Condition:   3,
		},
		{
			Name:        "Мастер организации",
			Description: "Создать 5 событий",
			Icon:        "💫",
			Points:      50,
			Type:        "event_creator",
			Condition:   5,
		},

		// Подписки на пользователей
		{
			Name:        "Знакомство",
			Description: "Подписаться на 1 пользователя",
			Icon:        "🦋",
			Points:      5,
			Type:        "social_subscription",
			Condition:   1,
		},
		{
			Name:        "Социальная активность",
			Description: "Подписаться на 3 пользователей",
			Icon:        "🌟",
			Points:      15,
			Type:        "social_subscription",
			Condition:   3,
		},
		{
			Name:        "Социальная бабочка",
			Description: "Подписаться на 5 пользователей",
			Icon:        "😊",
			Points:      25,
			Type:        "social_subscription",
			Condition:   5,
		},

		// Друзья
		{
			Name:        "Первый друг",
			Description: "Иметь 1 друга",
			Icon:        "🤗",
			Points:      10,
			Type:        "friends",
			Condition:   1,
		},
		{
			Name:        "Душа компании",
			Description: "Иметь 3 друзей",
			Icon:        "😄",
			Points:      30,
			Type:        "friends",
			Condition:   3,
		},
		{
			Name:        "Популярная личность",
			Description: "Иметь 5 друзей",
			Icon:        "🎉",
			Points:      50,
			Type:        "friends",
			Condition:   5,
		},

		// Подписки на события
		{
			Name:        "Любопытный",
			Description: "Подписаться на 1 событие",
			Icon:        "🔔",
			Points:      5,
			Type:        "event_subscriber",
			Condition:   1,
		},
		{
			Name:        "Энтузиаст",
			Description: "Подписаться на 3 события",
			Icon:        "📅",
			Points:      15,
			Type:        "event_subscriber",
			Condition:   3,
		},
		{
			Name:        "Профессиональный зритель",
			Description: "Подписаться на 5 событий",
			Icon:        "🎫",
			Points:      25,
			Type:        "event_subscriber",
			Condition:   5,
		},
	}

	createdCount := 0
	for _, achievement := range achievements {
		var existingAchievement models.Achievement
		err := r.db.Where("name = ?", achievement.Name).First(&existingAchievement).Error
		if err != nil {
			// Создаем новое достижение
			if err := r.db.Create(&achievement).Error; err != nil {
				return err
			}
			createdCount++
		}
	}

	return nil
}
