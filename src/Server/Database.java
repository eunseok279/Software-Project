package Server;

import java.sql.*;

public class Database {
    Connection con = null;
    Statement stmt = null;
    String url = "jdbc:mysql://localhost/dbstudy?serverTimezone=Asia/Seoul";    //dbstudy스키마
    String user = "root";
    String passwd = "dmstjr1534628!";        //MySQL에 저장한 root 계정의 비밀번호를 적어주면 된다.

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

    //테이블 생성
    void createTable() {
        try {
            String createStr = "CREATE TABLE user (name varchar(10) not null, id varchar(20) not null," + " password varchar(20) not null, PRIMARY KEY (id))";
            stmt.execute(createStr);
            System.out.println("테이블 생성 성공!");
        } catch (Exception e) {
            System.out.println("테이블 생성 실패 이유 : " + e.toString());
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

    //수정
    void changeData() {
        try {
            String changeStr = "UPDATE user SET name='가나다'";
            stmt.executeUpdate(changeStr);
            System.out.println("데이터 변경 성공!");
        } catch (Exception e) {
            System.out.println("데이터 변경 실패 이유 : " + e.toString());
        }
    }

    //조회
    void viewData() {
        try {
            System.out.println("== user 테이블 조회 ==");
            String viewStr1 = "SELECT * FROM user";
            ResultSet result1 = stmt.executeQuery(viewStr1);
            int cnt1 = 0;
            while (result1.next()) {
                System.out.print(result1.getString("name") + "\t" + result1.getString("id") + "\t" + result1.getString("password") + "\n");
                cnt1++;
            }

            System.out.println("");
            System.out.println("== students 테이블 조회 ==");
            String viewStr2 = "SELECT * FROM students";
            ResultSet result2 = stmt.executeQuery(viewStr2);
            int cnt2 = 0;
            while (result2.next()) {
                System.out.print(result2.getInt("number") + "\t" + result2.getString("name") + "\t" + result2.getString("gender") + "\t" + result2.getString("department") + "\n");
                cnt2++;
            }

            System.out.println("");
            System.out.println("데이터 조회 성공!");
        } catch (Exception e) {
            System.out.println("데이터 조회 실패 이유 : " + e.toString());
        }
    }
}
