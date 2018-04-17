package com.examstack.portal.controller.action;

import com.examstack.common.Constants;
import com.examstack.common.domain.exam.*;
import com.examstack.portal.security.UserInfo;
import com.examstack.portal.service.ExamPaperService;
import com.examstack.portal.service.ExamService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.codehaus.jackson.JsonProcessingException;
import org.springframework.amqp.AmqpException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

@Controller
public class ExamAction {
	
	@Autowired
	private ExamService examService;
	@Autowired
	private ExamPaperService examPaperService;
	
	@Autowired
	private org.springframework.amqp.core.AmqpTemplate qmqpTemplate;
	/**
	 * 获取答题卡
	 * @param histId
	 * @return
	 */
	@RequestMapping(value = "student/get-answersheet/{histId}", method = RequestMethod.GET)
	public @ResponseBody AnswerSheet getAnswerSheet(@PathVariable("histId") int histId){
		ExamHistory history = examService.getUserExamHistListByHistId(histId);
		Gson gson = new Gson();
		AnswerSheet answerSheet = gson.fromJson(history.getAnswerSheet(), AnswerSheet.class);
		return answerSheet;
	}
	
	/**
	 * 用户申请考试
	 * @param examId
	 * @return
	 */
	@RequestMapping(value = "student/exam/send-apply/{examId}", method = RequestMethod.GET)
	public @ResponseBody Message sendApply(@PathVariable("examId") int examId){
		Message msg = new Message();
		UserInfo userInfo = (UserInfo) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		try {
			Exam exam = this.checkExam(examId);
			//申请考试默认未审核
			examService.addUserExamHist(userInfo.getUserid(), examId, exam.getExamPaperId(),0);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			msg.setResult(e.getMessage());
			e.printStackTrace();
		}
		return msg;
	}
	
	/**
	 * 检查用户是否可以申请该考试
	 * @param examId
	 * @return
	 * @throws Exception
	 */
	public Exam checkExam(int examId) throws Exception{
		Exam exam = examService.getExamById(examId);
		if(exam == null){
			throw new Exception("考试不存在！或已经申请过一次！");
		}
		/*if(exam.getEffTime().before(new Date()))
			throw new Exception("不能在考试开始后申请！");*/
		if(exam.getApproved() != 1){
			throw new Exception("不能申请一个未审核通过的考试！");
		}
		
		return exam;
	}
	
	@RequestMapping(value = "/student/exam-submit", method = RequestMethod.POST)
	public @ResponseBody Message finishExam(@RequestBody AnswerSheet answerSheet) {

		Message message = new Message();

		try {

			/**
			 * 使用消息模式
			 */
			//ObjectMapper om = new ObjectMapper();
			//qmqpTemplate.convertAndSend(Constants.ANSWERSHEET_DATA_QUEUE, om.writeValueAsBytes(answerSheet));

			/**
			 * 使用非消息模式
			 */
			//1、获得ExamPaper
			ExamPaper examPaper = examPaperService.getExamPaperById(answerSheet.getExamPaperId());
			//2、获得ExamPaper的正确答案放到target中。
			Gson gson = new Gson();
			AnswerSheet target = gson.fromJson(examPaper.getAnswer_sheet(),AnswerSheet.class);
			//3、将标准答案放到answerMap中
			HashMap<Integer,AnswerSheetItem> answerMap = new HashMap<Integer,AnswerSheetItem>();
			for(AnswerSheetItem item : target.getAnswerSheetItems()){
				answerMap.put(item.getQuestionId(), item);
			}
			//4、设置answerSheet的最大分数
			answerSheet.setPointMax(target.getPointMax());
			//5、处理每个答案计算分类并设置是否正确。
			for(AnswerSheetItem item : answerSheet.getAnswerSheetItems()){
				if(item.getAnswer().equals(answerMap.get(item.getQuestionId()).getAnswer())){
					answerSheet.setPointRaw(answerSheet.getPointRaw() + answerMap.get(item.getQuestionId()).getPoint());
					item.setPoint(answerMap.get(item.getQuestionId()).getPoint());
					item.setRight(true);
				}
			}
			//6、将处理结果写入考试历史中
			List<AnswerSheetItem> itemList = answerSheet.getAnswerSheetItems();

			//全部是客观题，则状态更改为已阅卷
			int approved = 3;
			for(AnswerSheetItem item : itemList){
				if(item.getQuestionTypeId() != 1 && item.getQuestionTypeId() != 2 && item.getQuestionTypeId() != 3){
					approved = 2;
					break;
				}
			}
			//Gson gson = new Gson();
			examService.updateUserExamHist(answerSheet, gson.toJson(answerSheet),approved);

		} catch (AmqpException e) {
			e.printStackTrace();
			message.setResult("交卷失败");
			message.setMessageInfo(e.toString());
		}
		/**
		 * 使用消息模式
		 */
		/*catch (com.fasterxml.jackson.core.JsonProcessingException e) {
			e.printStackTrace();
			message.setResult("交卷失败");
			message.setMessageInfo(e.toString());
		}*/
		return message;
	}
	
	
	@RequestMapping(value = "addAnswerSheet4Test", method = RequestMethod.GET)
	public @ResponseBody Message addAnswerSheet4Test(Model model) throws JsonProcessingException, IOException {
		Message msg = new Message();
		AnswerSheet as = new AnswerSheet();
		as.setExamPaperId(2);
		ObjectMapper om = new ObjectMapper();
		qmqpTemplate.convertAndSend(Constants.ANSWERSHEET_DATA_QUEUE, om.writeValueAsBytes(as));
		return msg;
	}
}
