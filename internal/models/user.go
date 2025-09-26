package models

import (
	"gorm.io/gorm"
	"time"
)

type User struct {
	ID          uint           `gorm:"primaryKey" json:"id"`
	Email       string         `gorm:"uniqueIndex;not null" json:"email"`
	Password    string         `gorm:"not null" json:"-"`
	FirstName   string         `gorm:"not null" json:"first_name"`
	LastName    string         `gorm:"not null" json:"last_name"`
	Gender      string         `gorm:"size:10" json:"gender"`
	Age         int            `json:"age"`
	Phone       string         `json:"phone"`
	SocialLinks string         `gorm:"type:text" json:"social_links"`
	CreatedAt   time.Time      `gorm:"autoCreateTime" json:"created_at"`
	UpdatedAt   time.Time      `gorm:"autoUpdateTime" json:"updated_at"`
	DeletedAt   gorm.DeletedAt `gorm:"index" json:"-"`
}

type CreateUserRequest struct {
	Email       string `form:"email" binding:"required"`
	Password    string `form:"password" binding:"required"`
	FirstName   string `form:"first_name" binding:"required"`
	LastName    string `form:"last_name" binding:"required"`
	Gender      string `form:"gender"`
	Age         int    `form:"age"`
	Phone       string `form:"phone"`
	SocialLinks string `form:"social_links"`
}
