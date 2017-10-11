Feature: login

  Background:
    Given there are 15 users logged in

  Scenario:
    When a new user logs in
    Then the number of users should now be 16
    And the message should be
    """
    Welcome to the platform
    There are now 16 users logged into the system
    """

  Scenario:
    When a user logs out
    Then the number of users should now be 14
