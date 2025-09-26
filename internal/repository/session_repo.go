package repository

import (
	"event_social_platform/internal/models"
	"gorm.io/gorm"
	"time"
)

type SessionRepository struct {
	db *gorm.DB
}

func NewSessionRepository(db *gorm.DB) *SessionRepository {
	return &SessionRepository{db: db}
}

func (r *SessionRepository) CreateSession(session *models.Session) error {
	println("СОЗДАНИЕ СЕССИИ для UserID:", session.UserID, "Token:", session.Token)

	// Удаляем ВСЕ старые сессии для этого пользователя
	result := r.db.Where("user_id = ?", session.UserID).Delete(&models.Session{})
	println("Удалено старых сессий:", result.RowsAffected)

	err := r.db.Create(session).Error
	if err != nil {
		println("Ошибка создания сессии:", err.Error())
	} else {
		println("Сессия создана успешно")
	}
	return err
}

func (r *SessionRepository) GetSessionByToken(token string) (*models.Session, error) {
	println("ПОИСК СЕССИИ по токену:", token)

	var session models.Session
	err := r.db.Where("token = ?", token).First(&session).Error

	if err != nil {
		println("Сессия не найдена")
		return nil, err
	}

	println("Сессия найдена - UserID:", session.UserID, "Expires:", session.ExpiresAt.Format("15:04:05"))
	return &session, nil
}

func (r *SessionRepository) DeleteSession(token string) error {
	println("УДАЛЕНИЕ СЕССИИ по токену:", token)
	return r.db.Where("token = ?", token).Delete(&models.Session{}).Error
}

func (r *SessionRepository) CleanExpiredSessions() error {
	println("Очистка просроченных сессий")
	return r.db.Where("expires_at < ?", time.Now()).Delete(&models.Session{}).Error
}

// Для отладки - посмотреть все сессии
func (r *SessionRepository) DebugSessions() {
	var sessions []models.Session
	r.db.Find(&sessions)

	println("=== ДЕБАГ СЕССИЙ ===")
	for i, s := range sessions {
		println(i+1, "-> UserID:", s.UserID, "Token:", s.Token, "Expires:", s.ExpiresAt.Format("15:04:05"))
	}
	println("===================")
}
