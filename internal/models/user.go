package models

import (
	"golang.org/x/crypto/bcrypt"
	"gorm.io/gorm"
	"time"
)

type User struct {
	ID        uint      `gorm:"primaryKey" json:"id"`
	Email     string    `gorm:"uniqueIndex;not null" json:"email"`
	Password  string    `gorm:"not null" json:"-"`
	FirstName string    `gorm:"not null" json:"first_name"`
	LastName  string    `gorm:"not null" json:"last_name"`
	Gender    string    `gorm:"size:10" json:"gender"`
	BirthDate time.Time `gorm:"index" json:"birth_date"`
	Phone     string    `json:"phone"`
	//SocialLinks string         `gorm:"type:text" json:"social_links"`
	City      string         `gorm:"size:100" json:"city"`
	Latitude  float64        `gorm:"index" json:"latitude"`
	Longitude float64        `gorm:"index" json:"longitude"`
	CreatedAt time.Time      `gorm:"autoCreateTime" json:"created_at"`
	UpdatedAt time.Time      `gorm:"autoUpdateTime" json:"updated_at"`
	DeletedAt gorm.DeletedAt `gorm:"index" json:"-"`

	// Поля для статистики (не сохраняются в БД)
	FollowersCount int           `json:"followers_count,omitempty"`
	FollowingCount int           `json:"following_count,omitempty"`
	FriendsCount   int           `json:"friends_count,omitempty"`
	SocialLinks    []*SocialLink `json:"social_links,omitempty"`
	Posts          []*WallPost   `json:"posts,omitempty"`
	AgeText        string        `json:"age_text,omitempty"` // Текст для правильного склонения возраста
}

// HashPassword хеширует пароль перед сохранением
func (u *User) HashPassword(password string) error {
	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
	if err != nil {
		return err
	}
	u.Password = string(hashedPassword)
	return nil
}

// CheckPassword проверяет пароль
func (u *User) CheckPassword(password string) bool {
	err := bcrypt.CompareHashAndPassword([]byte(u.Password), []byte(password))
	return err == nil
}

// GetAge возвращает возраст пользователя на основе даты рождения
func (u *User) GetAge() int {
	if u.BirthDate.IsZero() {
		return 0
	}
	now := time.Now()
	age := now.Year() - u.BirthDate.Year()
	if now.Month() < u.BirthDate.Month() || (now.Month() == u.BirthDate.Month() && now.Day() < u.BirthDate.Day()) {
		age--
	}
	return age
}

type CreateUserRequest struct {
	Email     string  `form:"email" binding:"required,email"`
	Password  string  `form:"password" binding:"required"`
	FirstName string  `form:"first_name" binding:"required"`
	LastName  string  `form:"last_name" binding:"required"`
	Gender    string  `form:"gender"`
	BirthDate string  `form:"birth_date"`
	Phone     string  `form:"phone"`
	City      string  `form:"city"`
	Latitude  float64 `form:"latitude"`
	Longitude float64 `form:"longitude"`
	//SocialLinks string `form:"social_links"`
}

type UpdateUserRequest struct {
	FirstName string  `form:"first_name" binding:"required"`
	LastName  string  `form:"last_name" binding:"required"`
	Gender    string  `form:"gender"`
	BirthDate string  `form:"birth_date"`
	Phone     string  `form:"phone"`
	City      string  `form:"city"`
	Latitude  float64 `form:"latitude"`
	Longitude float64 `form:"longitude"`
	//SocialLinks string `form:"social_links"`
}
