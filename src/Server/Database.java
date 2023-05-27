package Server;

import java.sql.*;

public class Database {
    Connection con = null;
    Statement stmt = null;
    String url = "jdbc:mysql://18.191.149.53/testdb?serverTimezone=Asia/Seoul";    //dbstudy스키마
    String user = "tester";
    String passwd = "test";        //MySQL에 저장한 root 계정의 비밀번호를 적어주면 된다.

    public Database() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(url, user, passwd);
            stmt = con.createStatement();
            System.out.println("MySQL Server Open!!");
        } catch (Exception e) {
            System.out.println("MySQL Server is Down > " + e.toString());
        }
    }

    //삽입
    void insertUser(String userName,int money) {
        try {
            String insertStr = "INSERT INTO users (name, money) VALUES('" + userName + "','"+money+"')";
            stmt.executeUpdate(insertStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    boolean checkUser(String userName) {
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

    int getUserMoney(String userName){
        int money = 0;
        try {
            String checkMoney = "SELECT money FROM users WHERE name= '" + userName + "'";
            ResultSet result = stmt.executeQuery(checkMoney);
            if(result.next()){
              money = result.getInt("money");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return money;
    }


    //삭제
    void removeData() {
        try {
            String removeStr = "DELETE FROM students where name='이지수'";
            stmt.executeUpdate(removeStr);
            System.out.println("데이터 삭제 성공!");
        } catch (Exception e) {
            System.out.println("데이터 삭제 실패 이유 : " + e.toString());
        }
    }
}
