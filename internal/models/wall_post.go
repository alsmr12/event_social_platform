package models

import (
	"gorm.io/gorm"
	"time"
)

type WallPost struct {
	ID        uint           `gorm:"primaryKey" json:"id"`
	Content   string         `gorm:"type:text;not null" json:"content"`
	AuthorID  uint           `gorm:"not null" json:"author_id"`
	Author    User           `gorm:"foreignKey:AuthorID" json:"author"`
	UserID    uint           `gorm:"not null" json:"user_id"` // Владелец стены
	User      User           `gorm:"foreignKey:UserID" json:"user"`
	CreatedAt time.Time      `gorm:"autoCreateTime" json:"created_at"`
	UpdatedAt time.Time      `gorm:"autoUpdateTime" json:"updated_at"`
	DeletedAt gorm.DeletedAt `gorm:"index" json:"-"`
}

type CreateWallPostRequest struct {
	Content string `form:"content" binding:"required,max=1000"`
	UserID  uint   `form:"user_id" binding:"required"` // ID владельца стены
}
