package models

import (
	"gorm.io/gorm"
	"time"
)

type Friendship struct {
	ID uint `gorm:"primaryKey" json:"id"`

	UserID   uint `gorm:"not null" json:"user_id"`   // Инициатор запроса
	FriendID uint `gorm:"not null" json:"friend_id"` // Получатель запроса

	User   User `gorm:"foreignKey:UserID" json:"user"`
	Friend User `gorm:"foreignKey:FriendID" json:"friend"`

	Status    string         `gorm:"not null;default:'pending'" json:"status"` // pending, accepted, rejected
	CreatedAt time.Time      `gorm:"autoCreateTime" json:"created_at"`
	UpdatedAt time.Time      `gorm:"autoUpdateTime" json:"updated_at"`
	DeletedAt gorm.DeletedAt `gorm:"index" json:"-"`
}

type FriendshipStatus string

const (
	FriendshipPending  FriendshipStatus = "pending"
	FriendshipAccepted FriendshipStatus = "accepted"
	FriendshipRejected FriendshipStatus = "rejected"
)
