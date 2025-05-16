const mongoose = require('mongoose');

const workoutSchema = new mongoose.Schema({
  userId: {
    type: String,
    required: true
  },
  startTime: {
    type: Date,
    required: true
  },
  endTime: {
    type: Date,
    required: true
  },
  distance: {
    type: Number,
    required: true
  },
  duration: {
    type: Number,  // in milliseconds
    required: true
  },
  steps: {
    type: Number,
    required: true
  },
  averageSpeed: {
    type: Number,
    required: true
  },
  calories: {
    type: Number,
    required: true
  },
  averageHeartRate: {
    type: Number,
    required: true
  },
  maxHeartRate: {
    type: Number,
    required: true
  },
  locations: [{
    latitude: Number,
    longitude: Number,
    timestamp: Date
  }]
}, {
  timestamps: true
});

module.exports = mongoose.model('Workout', workoutSchema); 