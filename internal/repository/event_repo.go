package repository

import (
	"event_social_platform/internal/models"
	"fmt"
	"gorm.io/gorm"
	"math"
	"time"
)

type EventRepository struct {
	db *gorm.DB
}

func NewEventRepository(db *gorm.DB) *EventRepository {
	return &EventRepository{db: db}
}

// Параметры фильтрации
type EventFilter struct {
	Type      string
	DateFrom  time.Time
	DateTo    time.Time
	Latitude  float64
	Longitude float64
	Radius    float64 // в километрах
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

// GetEventByPrivateKey получает событие по приватному ключу
func (r *EventRepository) GetEventByPrivateKey(privateKey string) (*models.Event, error) {
	var event models.Event
	err := r.db.Preload("Creator").Where("private_key = ?", privateKey).First(&event).Error
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

	// Для приватных событий проверяем ТОЛЬКО прямые способы доступа

	// 1. Создатель события всегда имеет доступ
	if event.CreatorID == userID {
		return true, nil
	}

	// 2. Проверяем подписку на само событие - это ЕДИНСТВЕННЫЙ способ доступа для других пользователей
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

	// 3. Дружба и подписка на пользователя НЕ дают доступ к приватным событиям
	// 4. Если ничего не подошло - доступа нет
	return false, nil
}

// Получить события с фильтрацией
func (r *EventRepository) GetEventsWithFilter(filter EventFilter) ([]*models.Event, error) {
	var events []*models.Event
	query := r.db.Preload("Creator")

	// Фильтр по типу
	if filter.Type != "" && filter.Type != "all" {
		query = query.Where("type = ?", filter.Type)
	}

	// Фильтр по дате "от"
	if !filter.DateFrom.IsZero() {
		query = query.Where("date_time >= ?", filter.DateFrom)
	}

	// Фильтр по дате "до"
	if !filter.DateTo.IsZero() {
		query = query.Where("date_time <= ?", filter.DateTo)
	}

	// Если не указаны даты, показываем предстоящие события
	if filter.DateFrom.IsZero() && filter.DateTo.IsZero() {
		query = query.Where("date_time >= ?", time.Now()).Order("date_time ASC")
	} else {
		query = query.Order("date_time ASC")
	}

	err := query.Find(&events).Error
	if err != nil {
		return nil, err
	}

	// Фильтр по радиусу (если указан радиус)
	if filter.Radius > 0 {
		events = r.filterByRadius(events, filter.Latitude, filter.Longitude, filter.Radius)
	}

	return events, nil
}

// Фильтрация по радиусу
func (r *EventRepository) filterByRadius(events []*models.Event, userLat, userLng, radius float64) []*models.Event {
	var filtered []*models.Event
	for _, event := range events {
		if event.Latitude == 0 && event.Longitude == 0 {
			continue // События без координат пропускаем
		}

		distance := CalculateDistance(userLat, userLng, event.Latitude, event.Longitude)
		// ДОБАВЛЯЕМ ПРОВЕРКУ РАДИУСА
		if distance <= radius {
			filtered = append(filtered, event)
		}
	}
	return filtered
}

// Вычисление расстояния между двумя точками (формула гаверсинусов)
// Делаем функцию публичной, чтобы использовать в handler
func CalculateDistance(lat1, lng1, lat2, lng2 float64) float64 {
	const R = 6371 // Радиус Земли в км

	// Переводим градусы в радианы
	lat1Rad := lat1 * math.Pi / 180
	lng1Rad := lng1 * math.Pi / 180
	lat2Rad := lat2 * math.Pi / 180
	lng2Rad := lng2 * math.Pi / 180

	// Разница координат
	dlat := lat2Rad - lat1Rad
	dlng := lng2Rad - lng1Rad

	// Формула гаверсинусов
	a := math.Sin(dlat/2)*math.Sin(dlat/2) +
		math.Cos(lat1Rad)*math.Cos(lat2Rad)*
			math.Sin(dlng/2)*math.Sin(dlng/2)
	c := 2 * math.Atan2(math.Sqrt(a), math.Sqrt(1-a))

	return R * c
}

// Получить все типы событий для фильтра
func (r *EventRepository) GetEventTypes() ([]string, error) {
	var types []string
	err := r.db.Model(&models.Event{}).
		Distinct("type").
		Pluck("type", &types).Error
	return types, err
}

func (r *EventRepository) GetAllEvents() ([]*models.Event, error) {
	var events []*models.Event
	err := r.db.Preload("Creator").Order("date_time ASC").Find(&events).Error
	if err != nil {
		return nil, err
	}
	return events, nil
}

// Получить предстоящие события (ВСЕ - и публичные и приватные)
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

func (r *EventRepository) GetUserEventsCount(userID uint, count *int64) error {
	return r.db.Model(&models.Event{}).
		Where("creator_id = ?", userID).
		Count(count).Error
}

func (r *EventRepository) GetUserEventsCountAndroid(userID uint) (int64, error) {
	var count int64
	err := r.db.Model(&models.Event{}).Where("creator_id = ?", userID).Count(&count).Error
	return count, err
}

func (r *EventRepository) GetEventsFromUsers(userIDs []uint, limit, offset int) ([]*models.Event, error) {
	var events []*models.Event
	err := r.db.Preload("Creator").
		Where("creator_id IN (?)", userIDs).
		Order("created_at DESC").
		Limit(limit).
		Offset(offset).
		Find(&events).Error
	if err != nil {
		return nil, err
	}
	return events, nil
}

// Получить события текущего пользователя
func (r *EventRepository) GetUserEvents(userID uint) ([]*models.Event, error) {
	var events []*models.Event
	err := r.db.Preload("Creator").
		Where("creator_id = ?", userID).
		Order("created_at DESC").
		Find(&events).Error
	if err != nil {
		return nil, err
	}
	return events, nil
}
func (r *EventRepository) GetDB() *gorm.DB {
	return r.db
}
