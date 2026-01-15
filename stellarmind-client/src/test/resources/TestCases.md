# Text Test Cases for Test to SQL Generation using Gen AI
These test cases are designed to evaluate Gen AI's ability to generate correct SQL queries from natural language prompts based on the bs-schema database structure.
## Basic Query Tests
### Test Case 1: Simple Selection
**Prompt**: Retrieve all profile names from the profile table. **Expected Result**: SQL query that selects all profile names from the bs.profile table.
### Test Case 2: Filtered Selection
**Prompt**: Find all active profiles in the system. **Expected Result**: SQL query that retrieves all active profiles (where active = true).
### Test Case 3: Sorting Results
**Prompt**: Show me all profiles ordered by their creation date from newest to oldest. **Expected Result**: SQL query that selects profiles ordered by creation_date in descending order.
### Test Case 4: Limiting Results
**Prompt**: List the 5 most recently created vehicles. **Expected Result**: SQL query that retrieves 5 vehicles ordered by creation_date in descending order with a LIMIT clause.
## Data Filtering Tests
### Test Case 5: Multiple Conditions
**Prompt**: Find all active vehicles with architecture 'ABC123' created after January 1, 2023. **Expected Result**: SQL query with multiple WHERE conditions for active status, architecture, and creation date.
### Test Case 6: Null Value Handling
**Prompt**: Retrieve all vehicles that don't have an assigned profile. **Expected Result**: SQL query that finds vehicles where profile_name IS NULL.
### Test Case 7: Partial Text Matching
**Prompt**: Show me all ECUs with serial numbers containing the sequence 'X7'. **Expected Result**: SQL query using LIKE operator to find partial matches in serial_number.
### Test Case 8: Range Queries
**Prompt**: List all events that occurred between March 15, 2023 and April 15, 2023. **Expected Result**: SQL query with date range conditions in the WHERE clause.
## Join Operations Tests
### Test Case 9: Simple Join
**Prompt**: Get all vehicles along with their profile descriptions. **Expected Result**: SQL query joining bs_vehicle and profile tables.
### Test Case 10: Multiple Joins
**Prompt**: List all ECUs with their vehicle information and associated profile details. **Expected Result**: SQL query with multiple joins across bs_ecu, bs_vehicle, and profile tables.
### Test Case 11: Left Join
**Prompt**: Show all vehicles and any ECUs they might have. **Expected Result**: SQL query with LEFT JOIN to include vehicles without ECUs.
### Test Case 12: Filtering After Join
**Prompt**: Find all active ECUs that belong to vehicles with European region code. **Expected Result**: SQL query with a join and multiple conditions across joined tables.
## Aggregation Tests
### Test Case 13: Simple Count
**Prompt**: How many vehicles are registered in the system? **Expected Result**: SQL query using COUNT to determine the number of vehicles.
### Test Case 14: Grouping
**Prompt**: Count the number of vehicles for each profile. **Expected Result**: SQL query with GROUP BY on profile_name and COUNT.
### Test Case 15: Multiple Aggregation Functions
**Prompt**: For each vehicle region, show the count of vehicles and the earliest and latest creation dates. **Expected Result**: SQL query with GROUP BY and multiple aggregation functions (COUNT, MIN, MAX).
### Test Case 16: Having Clause
**Prompt**: Find profiles that are associated with more than 3 vehicles. **Expected Result**: SQL query with GROUP BY and HAVING clause to filter based on count.
## Data Modification Tests
### Test Case 17: Insert Operation
**Prompt**: Add a new profile named 'TELEMATICS_BASIC' with active status true and a description 'Basic telematics configuration'. **Expected Result**: SQL INSERT statement with appropriate columns and values.
### Test Case 18: Update Operation
**Prompt**: Update the description for the profile named 'TELEMATICS_PREMIUM' to 'Premium telematics with enhanced features'. **Expected Result**: SQL UPDATE statement with the correct WHERE condition.
### Test Case 19: Delete Operation
**Prompt**: Remove all events with 'ERROR' status that occurred before 2022. **Expected Result**: SQL DELETE statement with appropriate filtering conditions.
### Test Case 20: Conditional Update
**Prompt**: Mark all vehicles as inactive if they haven't been updated in the last 6 months. **Expected Result**: SQL UPDATE statement with date comparison logic.
## JSON Data Tests
### Test Case 21: JSON Property Access
**Prompt**: Find profiles where the profile_object contains a property named 'version'. **Expected Result**: SQL query using PostgreSQL JSON operators to check for the existence of a key.
### Test Case 22: JSON Value Extraction
**Prompt**: Extract the 'timeout' value from the profile_object for all active profiles. **Expected Result**: SQL query that extracts a specific value from the JSONB data.
### Test Case 23: JSON Filtering
**Prompt**: Find all profiles where the profile_object has a 'priority' value greater than 5. **Expected Result**: SQL query that filters based on a numeric value inside JSON data.
## Subquery Tests
### Test Case 24: Simple Subquery
**Prompt**: List all vehicles that are associated with active profiles. **Expected Result**: SQL query with a subquery or join to filter vehicles based on profile status.
### Test Case 25: Correlated Subquery
**Prompt**: Find all profiles that don't have any associated vehicles. **Expected Result**: SQL query using a correlated subquery or equivalent join with appropriate conditions.
## Advanced Filtering Tests
### Test Case 26: Date Functions
**Prompt**: Find all events created in the last 30 days. **Expected Result**: SQL query using date arithmetic functions.
### Test Case 27: String Functions
**Prompt**: List all profiles with names converted to uppercase. **Expected Result**: SQL query using string manipulation functions.
### Test Case 28: Complex Filtering
**Prompt**: Find vehicles created in 2023 with architecture 'ABC123' or 'XYZ789' that have at least one ECU. **Expected Result**: SQL query with complex WHERE conditions and existence check.
## Schema Understanding Tests
### Test Case 29: Column Existence
**Prompt**: List all columns in the bs_vehicle table. **Expected Result**: SQL query that retrieves column information from information_schema.
### Test Case 30: Relationship Understanding
**Prompt**: Show me the relationship between vehicles and ECUs. **Expected Result**: SQL query that demonstrates understanding of the foreign key relationship.
## Error Handling Tests
### Test Case 31: Non-existent Column
**Prompt**: Find vehicles by their color. **Expected Result**: Recognition that 'color' is not a column in the schema and appropriate error message.
### Test Case 32: Incorrect Join Conditions
**Prompt**: Join the event table with the profile table using serial_number. **Expected Result**: Recognition that these tables cannot be joined directly with these columns and suggestion of a valid join path.
## Performance Awareness Tests
### Test Case 33: Index Usage
**Prompt**: Find the most efficient way to query events by serial_number. **Expected Result**: SQL query that demonstrates awareness of the existing index on serial_number.
### Test Case 34: Query Optimization
**Prompt**: Write an optimized query to find all active vehicles with their ECUs and profiles. **Expected Result**: SQL query that shows understanding of efficient join ordering and filtering.
## Security Awareness Tests
### Test Case 35: SQL Injection Prevention
**Prompt**: Find a profile with name "PROFILE_1'; DROP TABLE bs.profile; --". **Expected Result**: SQL query that properly handles the potential injection attack by using parameterized queries or appropriate escaping.
