const mongoose = require('mongoose');
const dotenv = require('dotenv');
const Workout = require('./models/Workout');

dotenv.config();

async function testDatabase() {
  try {
    // Connect to MongoDB
    await mongoose.connect(process.env.MONGODB_URI);
    console.log('Connected to MongoDB');

    // Create a test workout
    const testWorkout = new Workout({
      userId: 'test-user-123',
      startTime: new Date(),
      endTime: new Date(Date.now() + 3600000), // 1 hour later
      distance: 5.2,
      duration: 3600000,
      steps: 6500,
      averageSpeed: 5.2,
      calories: 320,
      averageHeartRate: 145,
      maxHeartRate: 180,
      locations: [
        {
          latitude: 40.7128,
          longitude: -74.0060,
          timestamp: new Date()
        }
      ]
    });

    // Save the workout
    const savedWorkout = await testWorkout.save();
    console.log('Created test workout:', savedWorkout._id);

    // Read the workout
    const foundWorkout = await Workout.findById(savedWorkout._id);
    console.log('Found workout:', foundWorkout._id);

    // Update the workout
    const updatedWorkout = await Workout.findByIdAndUpdate(
      savedWorkout._id,
      { calories: 350 },
      { new: true }
    );
    console.log('Updated calories:', updatedWorkout.calories);

    // Delete the workout
    await Workout.findByIdAndDelete(savedWorkout._id);
    console.log('Deleted test workout');

    console.log('All CRUD operations successful!');
  } catch (error) {
    console.error('Test failed:', error);
  } finally {
    // Close the connection
    await mongoose.connection.close();
    console.log('Database connection closed');
  }
}

// Run the test
testDatabase(); 