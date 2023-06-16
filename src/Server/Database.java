package Server;

import java.sql.*;

public class Database {
    Connection con = null;
    Statement stmt = null;
    String url = "jdbc:mysql://18.191.149.53/testdb?serverTimezone=Asia/Seoul";
    String user = "tester";
    String passwd = "test";

    public Database() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(url, user, passwd);
            stmt = con.createStatement();
            System.out.println("MySQL Server Open!!");
        } catch (Exception e) {
            System.out.println("MySQL Server is Down > " + e);
        }
    }

    //삽입
    void insertUser(String userName) { // 유저 추가
        try {
            String insertStr = "INSERT INTO users (name, money) VALUES('" + userName + "','" + 200 + "')";
            stmt.executeUpdate(insertStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    boolean checkUser(String userName) { // 유저 확인
        boolean flag = false;

        try {
            String checkingStr = "SELECT name FROM users WHERE name='" + userName + "'";
            ResultSet result = stmt.executeQuery(checkingStr);

            while (result.next()) {
                if (userName.equals(result.getString("name"))) flag = true;
                else flag = false;
            }
        } catch (Exception e) {
            flag = false;
        }
        return flag;
    }

    int getUserMoney(String userName) { // 유저 소지금 확인
        int money = 0;
        try {
            String checkMoney = "SELECT money FROM users WHERE name= '" + userName + "'";
            ResultSet result = stmt.executeQuery(checkMoney);
            if (result.next()) {
                money = result.getInt("money");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return money;
    }


    //삭제
    public void updateUserBalance(String userName, int money) throws SQLException { // 유저 소지금 업데이트
        String sqlUpdate = "UPDATE users SET money = ? WHERE name = ?";

        PreparedStatement pstmt = con.prepareStatement(sqlUpdate);

        pstmt.setInt(1, money);
        pstmt.setString(2, userName);

        int rowAffected = pstmt.executeUpdate();
        if (rowAffected == 1) {
            System.out.println(userName+">> Successfully updated the balance.");
        } else {
            System.out.println("Failed to update the balance or user not found.");
        }
    }
}
