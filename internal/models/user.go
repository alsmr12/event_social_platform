package models

import (
	"encoding/json"
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
	FollowersCount int           `gorm:"-" json:"followers_count,omitempty"`
	FollowingCount int           `gorm:"-" json:"following_count,omitempty"`
	FriendsCount   int           `gorm:"-" json:"friends_count,omitempty"`
	SocialLinks    []*SocialLink `gorm:"-" json:"social_links,omitempty"`
	Posts          []*WallPost   `gorm:"-" json:"posts,omitempty"`
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

// MarshalJSON кастомная сериализация для добавления возраста
func (u *User) MarshalJSON() ([]byte, error) {
	type Alias User
	return json.Marshal(&struct {
		*Alias
		Age         int    `json:"age"`
		BirthDate   string `json:"birth_date,omitempty"`
		BirthDateDB string `json:"-"`
	}{
		Alias:       (*Alias)(u),
		Age:         u.GetAge(),
		BirthDate:   u.FormatBirthDate(),
		BirthDateDB: u.BirthDate.Format("2006-01-02"),
	})
}

// FormatBirthDate возвращает дату рождения в формате строки
func (u *User) FormatBirthDate() string {
	if u.BirthDate.IsZero() {
		return ""
	}
	return u.BirthDate.Format("2006-01-02")
}

type CreateUserRequest struct {
	Email     string  `form:"email" json:"email" binding:"required,email"`
	Password  string  `form:"password" json:"password" binding:"required"`
	FirstName string  `form:"first_name" json:"first_name" binding:"required"`
	LastName  string  `form:"last_name" json:"last_name" binding:"required"`
	Gender    string  `form:"gender" json:"gender"`
	BirthDate string  `form:"birth_date" json:"birth_date"` // Формат: "2006-01-02"
	Phone     string  `form:"phone" json:"phone"`
	City      string  `form:"city" json:"city"`
	Latitude  float64 `form:"latitude" json:"latitude"`
	Longitude float64 `form:"longitude" json:"longitude"`
	//SocialLinks string `form:"social_links" json:"social_links"`
}

type UpdateUserRequest struct {
	FirstName string  `form:"first_name" json:"first_name" binding:"required"`
	LastName  string  `form:"last_name" json:"last_name" binding:"required"`
	Gender    string  `form:"gender" json:"gender"`
	BirthDate string  `form:"birth_date" json:"birth_date"` // Формат: "2006-01-02"
	Phone     string  `form:"phone" json:"phone"`
	City      string  `form:"city" json:"city"`
	Latitude  float64 `form:"latitude" json:"latitude"`
	Longitude float64 `form:"longitude" json:"longitude"`
	//SocialLinks string `form:"social_links" json:"social_links"`
}