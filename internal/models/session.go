package models

import (
	"time"
)

type Session struct {
	ID        uint      `gorm:"primaryKey" json:"id"`
	UserID    uint      `gorm:"not null" json:"user_id"`
	Token     string    `gorm:"uniqueIndex;not null" json:"token"`
	ExpiresAt time.Time `gorm:"not null" json:"expires_at"`
	CreatedAt time.Time `gorm:"autoCreateTime" json:"created_at"`
}

type LoginRequest struct {
	Email    string `form:"email" binding:"required,email"`
	Password string `form:"password" binding:"required"`
}
