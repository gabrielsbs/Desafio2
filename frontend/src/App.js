import React from 'react';
import $ from 'jquery';
import './App.css';
import img from './Images/img_lights.jpg'
import Header from './Components/Header';
import Paper from '@material-ui/core/Paper';
import Table from '@material-ui/core/Table';
import Collapse from '@material-ui/core/Collapse';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';
import DeleteIcon from '@material-ui/icons/Delete';
import ImageIcon from '@material-ui/icons/Image';
import IconButton from '@material-ui/core/IconButton';
import { withStyles } from '@material-ui/core/styles';

const styles = theme => ({
  root: {
    backgroundColor: '#f5f5f5',
    display: 'flex',
    height: '100vh',
    justifyContent: 'center',
    flexDirection: "column",
  },
  paper:{
    marginLeft: '10%',
    marginRight: '10%',
    borderRadius: 8,
    height: '90%',
    display: 'flex',
    justifyContent: 'center',
    flexDirection: "column",
    verticalAlign: 'middle'
  },
  tableRoot: {
    width: '80%',
    margin: 'auto', 
    marginTop: 15,
    overflowX: 'auto',
  },
  hideRow:{
    display:'none'
  },
  showRow:{
    fontWeight: 400
  },
  table: {
    minWidth: 650,
  },
}); 
const baseUrl = 'http://localhost:8080/tasks';

const initialState = {
  task: {id: 0, name: "", phase: "", imageLocation: "", latitude:"", longitude:""},
  list: [],
  displayList: [],
}
class App extends React.Component {
  state = {...initialState}

  constructor(props){
    super(props);
    this.newTask = this.newTask.bind(this);
    this.filterTask = this.filterTask.bind(this);
  }

  componentWillMount(){
    $.get(baseUrl,(tasks) =>{
      const displayTasks = tasks
      this.setState({
        list: tasks,
        displayList: displayTasks
      })
    })
  }

  postJSON(url, data, callback) {
    return $.ajax({
    headers: { 
        'Accept': 'application/json',
        'Content-Type': 'application/json' ,
        'Access-Control-Allow-Origin': true
    },
      'type': 'POST',
    'url': url,
    'crossDomain': true,
    'data': JSON.stringify(data),
    'dataType': 'json',
    'success': callback
    });
  };

  newTask(name){
    let task = this.state.task;
    
    task.name = name;
    task.phase = "Pendente";
    task.imageLocation = "";
    task.imgShow = false;
    this.postJSON(baseUrl, task, result =>{
      let listCopy = this.state.list;
      let displayListCopy = this.state.displayList;
      if(listCopy === displayListCopy){
        listCopy.push(result);
        displayListCopy = listCopy;
      }else{
        listCopy.push(result);
        displayListCopy.push(result);
      }
      this.setState({
        list: listCopy,
        displayList: displayListCopy,
      })
    })     
  };

  filterTask(option){
    console.log(option)
    let listCopy = this.state.list;
    
    if(option === "todas"){
      this.setState({
        displayList: listCopy
      })
    }else{
      listCopy = listCopy.filter(task =>{
        return task.phase === option
      })
      this.setState({
        displayList:listCopy
      })
    }
  }


  render (){
    const { classes } = this.props;

    const imageRow = function(row) {
      if(row.phase === "Concluída"){
        if(true){
          return(
            <TableRow  key={"Image"+row.id}>
              <TableCell className={row.imgShow?classes.showRow:classes.hideRow} colSpan={3} align = 'center'>
              <Collapse  in={row.imgShow} unmountOnExit={true}><img src={require(`${row.imageLocation}`)} alt={img} height="80%" width = "80%"/></Collapse>
              </TableCell>
            </TableRow>
          )
        }
      }
    }

    const showImage = (e,row) =>{
      row.imgShow = !row.imgShow;
      const index1 = this.state.displayList.findIndex(task =>task.id === row.id)
      const index2 = this.state.list.findIndex(task =>task.id === row.id)
      let displayListCopy = this.state.displayList
      let listCopy = this.state.list
      listCopy[index2] = row
      displayListCopy[index1] = row
      this.setState({
        list: listCopy,
        displayList: displayListCopy
      })
    }

    const deleteRow = (row) =>{
      const url = `${baseUrl}/${row.id}`;
      $.ajax({
        url: url,
        type: 'DELETE',
        success: result => {
          console.log(result)
          const list = this.state.list.filter(listItem => listItem.id !== row.id);
          const displayList = this.state.list.filter(listItem => listItem.id !== row.id);
          this.setState({
            list: list,
            displayList:displayList
          })
        }
    });
    }
    
    return(
      <div className={classes.root}>
        <Paper className={classes.paper} >
          <Header onSubmit = {this.newTask} onFilterSelected = {this.filterTask}/>
          <Paper className={classes.tableRoot}>
            <Table className={classes.table}>
              <TableHead>
                <TableRow>
                  <TableCell>Tarefa</TableCell>
                  <TableCell>Estágio</TableCell>
                  <TableCell  align= 'center'>Ações</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {this.state.displayList.map(function (row) {
                 
                  return (
                    <React.Fragment key = {row.id}>
                      <TableRow key={row.id}>
                        <TableCell component="th" scope="row">
                          {row.name}
                        </TableCell>
                        <TableCell>{row.phase}</TableCell>
                        <TableCell align= 'center'>
                          <IconButton onClick = {e => {deleteRow(row)}} className = 'iconButton' aria-label="search">
                              <DeleteIcon/>
                          </IconButton>
                          <IconButton value = {row} disabled = {row.phase==="Concluída"? false: true} className = 'iconButton' aria-label="search"  onClick = {e => {showImage(e,row)}}>
                              <ImageIcon/>
                          </IconButton>
                        </TableCell>
                      </TableRow>
                       {imageRow(row)}
                    </React.Fragment>
                  )
                }
                )}
              </TableBody>    
            </Table>
          </Paper>
        </Paper>
      </div>
      )
  }
}

export default withStyles(styles)(App);
